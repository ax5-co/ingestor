package com.boutiqaat.ingestor.service;

import com.boutiqaat.ingestor.entity.CatalogProductEntity;
import com.boutiqaat.ingestor.entity.CatalogProductEntityDecimal;
import com.boutiqaat.ingestor.feignClient.CrsClient;
import com.boutiqaat.ingestor.model.CountryModel;
import com.boutiqaat.ingestor.model.Decimal;
import com.boutiqaat.ingestor.model.PriceModel;
import com.boutiqaat.ingestor.model.SkuModel;
import com.boutiqaat.ingestor.repository.PriceRepository;
import com.boutiqaat.ingestor.repository.ProductRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Service purpose is to ingest prices info from magento DB into multi_stocks.price table.
 *
 * The ingestion is applied through CRS's upsert prices API: POST /price-info, respecting the validations and accurate
 * processing of the API.
 *
 * For Bundles with no bundle discount, i.e., no price (& special_price) attributes in magento table, the discount is
 * zero & the upsert API call needs to  be triggered for CRS to apply corresponding calculations.
 *
 * For Configurable products, there is no pricing records in magento table & CRS needs only a trigger to the API to
 * apply corresponding calculations.
 */
@Deprecated
@Slf4j(topic = "PRICES INGESTION")
@Service
@RequiredArgsConstructor
public class PricesIngestionService implements IngestionStreamWriter {
    private final ProductRepository productRepository;
    private final PriceRepository priceRepository;
    private final CrsClient crsClient;
    private final ObjectMapper mapper;


    @Transactional(readOnly = true)
    @Override
    public void ingest(final OutputStream outputStream, final int startPage, final int pageSize, boolean showSuccess, boolean showFailure) {
        int lastPage = Integer.MAX_VALUE;
        Pageable pageable;
        int page = Math.max(startPage, 0);
        try (PrintWriter output = new PrintWriter(outputStream, true)) {
            output.println(opening);
            AtomicLong successes = new AtomicLong();
            AtomicLong failures = new AtomicLong();
            while (page <= lastPage) {
                pageable = PageRequest.of(page, pageSize);
                Page<CatalogProductEntity> paged = productRepository.findAll(pageable);
                if (lastPage == Integer.MAX_VALUE) {
                    lastPage = paged.getTotalPages();
                }
                List<CatalogProductEntity> fetchedProducts = paged.getContent();
                log.info("Fetched page {} from CatalogProductEntity of size {} of total pages {}", page, pageSize, lastPage);
                final Map<Long, SkuModel> idToSkuModel = fetchedProducts.stream()
                        .peek(product -> log.debug("Fetched from cpe {}",product))
                        .map(product -> SkuModel
                                .builder()
                                .sku(product.getSku())
                                .productId(product.getProductId())
                                .description("")
                                .type(product.getType().toUpperCase())
                                .build())
                        .peek(skuModel -> log.debug("Mapped to SkuModel {}",skuModel))
                        .collect(Collectors.toMap(SkuModel::getProductId, skuModel -> skuModel));

                //Fetch Prices of the fetched products, provided in _decimal table
                Map<Long, PriceModel> toBeUpserted = priceRepository.findAllByProductIdIn(idToSkuModel.keySet())
                        .stream()
                        .filter(entity -> Decimal.supportedAttributes().contains(entity.getAttributeId())
                                && Objects.nonNull(entity.getAmount()))
                        .collect(Collectors.groupingBy(CatalogProductEntityDecimal::getProductId))
                        .entrySet()
                        .stream()
                        .map(entry ->
                                createPriceModel(idToSkuModel.get(entry.getKey()), entry.getValue()))
                        .collect(Collectors.toMap(model -> {
                            assert model != null;
                            return model.getProductId();
                        }, model -> model));

                //Create default priceModel for products without explicit pricing records -- CONFIGS & some BUNDLES
                toBeUpserted.putAll(idToSkuModel.entrySet()
                        .stream()
                        .filter(entry -> !toBeUpserted.containsKey(entry.getKey()))
                        .map(entry -> createPriceModel(entry.getValue(), Collections.emptyList()))
                        .collect(Collectors.toMap(model -> {
                            assert model != null;
                            return model.getProductId();
                        }, model -> model)));

                if(!toBeUpserted.isEmpty()) {
                    log.debug("Calling POST /price-info on productIds {}", toBeUpserted.keySet());
                    try {
                        crsClient.upsertPrices(new ArrayList<>(toBeUpserted.values()));
                        if (showSuccess) {
                            toBeUpserted.values().forEach(model -> {
                                successes.getAndIncrement();
                                writeSuccess(output, mapper, log, model, successes);
                            });
                        }
                    } catch (FeignException e) {
                        log.error("EXCEPTION in POST /price-info on models of ids {}, attempting one-by-one calls",
                                toBeUpserted.keySet());
                        toBeUpserted.values().forEach( model -> {
                            try {
                                crsClient.upsertPrices(Collections.singletonList(model));
                                if (showSuccess) {
                                    successes.getAndIncrement();
                                    writeSuccess(output, mapper, log, model, successes);
                                }
                            } catch (FeignException fe) {
                                log.error("Failure Feign Request Body {}",
                                        new String(fe.request().body(), StandardCharsets.UTF_8));
                                log.error("EXCEPTION in POST /price-info on model {}", model, fe);
                                if (showFailure) {
                                    failures.getAndIncrement();
                                    writeFailure(output, mapper, log, model, failures);
                                }
                            }
                        });
                    }
                }
                page++;
            }
            output.println(closing);
        }
    }

    private PriceModel createPriceModel(SkuModel productModel,
                                        List<CatalogProductEntityDecimal> priceEntities) {
        int defaultCurrencyPrecision = 3;
        BigDecimal discount = BigDecimal.ZERO, regularPrice = BigDecimal.ZERO, sellingPrice = BigDecimal.ZERO;
        long productId = productModel.getProductId();

        CatalogProductEntityDecimal price = priceEntities.stream()
                .filter(entity -> Objects.equals(entity.getProductId(), productId) &&
                        Objects.nonNull(Decimal.getByAttributeId(entity.getAttributeId())) &&
                        Objects.equals(Decimal.getByAttributeId(entity.getAttributeId()), Decimal.PRICE)
                )
                .findAny()
                .orElse(null);
        CatalogProductEntityDecimal specialPrice = priceEntities.stream()
                .filter(entity -> Objects.equals(entity.getProductId(), productId) &&
                        Objects.nonNull(Decimal.getByAttributeId(entity.getAttributeId())) &&
                        Objects.equals(Decimal.getByAttributeId(entity.getAttributeId()), Decimal.SPECIAL_PRICE)
                )
                .findAny()
                .orElse(null);

        if (productModel.getType().equalsIgnoreCase("bundle")) {
            if (specialPrice != null  && specialPrice.getAmount() != null
                    && specialPrice.getAmount().compareTo(BigDecimal.valueOf(100.00)) <= 0) {
                discount =BigDecimal.valueOf(100.00)
                        .subtract(specialPrice.getAmount())
                        .setScale(defaultCurrencyPrecision, RoundingMode.HALF_UP);
            }
        } else if (price != null && price.getAmount() != null) {
            regularPrice = price.getAmount();
            sellingPrice = price.getAmount();
            if (specialPrice != null && specialPrice.getAmount() != null
                    && price.getAmount().compareTo(specialPrice.getAmount()) > 0) {
                sellingPrice = specialPrice.getAmount();
                discount = specialPrice.getAmount()
                        .divide(price.getAmount(), defaultCurrencyPrecision, RoundingMode.HALF_UP)
                        .movePointRight(2);
            }
        }
        try {
            return PriceModel.builder()
                    .product(productModel)
                    .country(CountryModel.createDefault())
                    .productId(productId)
                    .price(regularPrice.setScale(defaultCurrencyPrecision, RoundingMode.HALF_UP))
                    .specialPrice(sellingPrice.setScale(defaultCurrencyPrecision, RoundingMode.HALF_UP))
                    .discount(discount.setScale(defaultCurrencyPrecision, RoundingMode.HALF_UP))
                    .taxes(BigDecimal.ZERO)
                    .build();
        } catch (RuntimeException e ) {
            log.error("Failed To Create a PriceModel with product {} & pricing-info {}", productModel, priceEntities);
            e.printStackTrace();
            return null;
        }
    }
}
