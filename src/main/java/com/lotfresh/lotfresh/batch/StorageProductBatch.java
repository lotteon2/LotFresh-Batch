package com.lotfresh.lotfresh.batch;

import com.lotfresh.lotfresh.domain.StorageProduct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JdbcPagingItemReader;
import org.springframework.batch.item.database.Order;
import org.springframework.batch.item.database.PagingQueryProvider;
import org.springframework.batch.item.database.support.SqlPagingQueryProviderFactoryBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Slf4j
@Configuration
public class StorageProductBatch {
    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final DataSource dataSource;
    private final int chunkSize = 3;

    @Bean
    public Job storageProductExpirationDeleteJob() throws Exception{
        return jobBuilderFactory.get("storageProductExpirationDeleteJob")
                .start(storageProductExpirationDeleteStep()).build();
    }

    @Bean
    public Step storageProductExpirationDeleteStep() throws Exception{
        return stepBuilderFactory.get("storageProductExpirationDeleteStep")
                .<StorageProduct, StorageProduct>chunk(chunkSize)
                .reader(expirationItemReader(null))
                .writer(expirationItemWriter())
                .build();
    }

    // date 형식은 20221021 이런식
    @Bean
    @StepScope
    public JdbcPagingItemReader<StorageProduct> expirationItemReader(@Value("#{jobParameters[date]}") String date) throws Exception{
        Map<String, Object> parameterValues = new HashMap<>();
        parameterValues.put("date", date);

        JdbcPagingItemReader<StorageProduct> itemReader = new JdbcPagingItemReader<StorageProduct>() {
            @Override
            public int getPage() {
                return 0; // 항상 첫 페이지만 읽도록 설정
            }
        };

        itemReader.setDataSource(dataSource);
        itemReader.setFetchSize(chunkSize);
        itemReader.setPageSize(chunkSize);
        itemReader.setRowMapper(new BeanPropertyRowMapper<>(StorageProduct.class));
        itemReader.setQueryProvider(createQueryProvider());
        itemReader.setParameterValues(parameterValues);
        itemReader.setName("expirationItemReader");

        return itemReader;
    }

    @Bean
    public ItemWriter<StorageProduct> expirationItemWriter() {
        return items -> {
            List<Long> idsToDelete = items.stream()
                    .map(StorageProduct::getId)
                    .collect(Collectors.toList());

            String deleteQuery = "DELETE FROM storage_product WHERE id IN (:ids)";

            MapSqlParameterSource parameters = new MapSqlParameterSource();
            parameters.addValue("ids", idsToDelete);

            jdbcTemplate.update(deleteQuery, parameters);
        };
    }

    @Bean
    public PagingQueryProvider createQueryProvider() throws Exception {
        SqlPagingQueryProviderFactoryBean queryProvider = new SqlPagingQueryProviderFactoryBean();
        queryProvider.setDataSource(dataSource);
        queryProvider.setSelectClause("id, expiration_date_end, expiration_date_start, product_id, stock, storage_id");
        queryProvider.setFromClause("from storage_product");
        queryProvider.setWhereClause("where expiration_date_end < :date");

        Map<String, Order> sortKeys = new HashMap<>(1);
        sortKeys.put("id", Order.ASCENDING);

        queryProvider.setSortKeys(sortKeys);

        return queryProvider.getObject();
    }
}