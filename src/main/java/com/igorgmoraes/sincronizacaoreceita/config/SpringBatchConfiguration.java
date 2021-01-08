package com.igorgmoraes.sincronizacaoreceita.config;

import com.igorgmoraes.sincronizacaoreceita.exception.InvalidFileException;
import com.igorgmoraes.sincronizacaoreceita.model.AccountInfo;
import com.igorgmoraes.sincronizacaoreceita.service.ReceitaService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.integration.async.AsyncItemProcessor;
import org.springframework.batch.integration.async.AsyncItemWriter;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.LineMapper;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.builder.FlatFileItemWriterBuilder;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.BeanWrapperFieldExtractor;
import org.springframework.batch.item.file.transform.DelimitedLineAggregator;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;


@Configuration
@EnableBatchProcessing
@Slf4j
public class SpringBatchConfiguration {

    @Bean
    public Job job(JobBuilderFactory jobBuilderFactory,
                   StepBuilderFactory stepBuilderFactory) throws InvalidFileException {

        Step step = stepBuilderFactory.get("Synchronize file")
                .<AccountInfo, Future<AccountInfo>>chunk(50000) //This chunk size should be changed to reach the best performance
                .reader(accountInfoItemReader(null))
                .processor(asyncProcessor())
                .writer(asyncWriter())
                .taskExecutor(taskExecutor())
                .build();

        return jobBuilderFactory.get("Synchronization Job")
                .incrementer(new RunIdIncrementer())
                .start(step)
                .build();
    }

    @Bean
    @StepScope
    public FlatFileItemReader<AccountInfo> accountInfoItemReader(@Value("#{jobParameters['fileName']}") String fileName)
            throws InvalidFileException {

        FileSystemResource resource = new FileSystemResource(fileName);
        if (!resource.isReadable())
            throw new InvalidFileException("Arquivo não pôde ser lido ou não existe.");

        return new FlatFileItemReaderBuilder<AccountInfo>()
                .resource(resource)
                .name("CSV-Reader")
                .linesToSkip(1)
                .lineMapper(lineMapper())
                .build();
    }


    @Bean
    public LineMapper<AccountInfo> lineMapper() {
        DefaultLineMapper<AccountInfo> defaultLineMapper = new DefaultLineMapper<>();
        DelimitedLineTokenizer lineTokenizer = new DelimitedLineTokenizer();

        lineTokenizer.setDelimiter(";");
        lineTokenizer.setStrict(true);
        lineTokenizer.setNames("agencia", "conta", "saldo", "status");

        BeanWrapperFieldSetMapper<AccountInfo> fieldSetMapper = new BeanWrapperFieldSetMapper<>();

        fieldSetMapper.setTargetType(AccountInfo.class);

        defaultLineMapper.setLineTokenizer(lineTokenizer);
        defaultLineMapper.setFieldSetMapper(fieldSetMapper);

        return defaultLineMapper;
    }

    @Bean
    public AsyncItemProcessor<AccountInfo, AccountInfo> asyncProcessor() {
        AsyncItemProcessor<AccountInfo, AccountInfo> asyncItemProcessor = new AsyncItemProcessor<>();
        asyncItemProcessor.setDelegate(itemProcessor());
        asyncItemProcessor.setTaskExecutor(taskExecutor());

        return asyncItemProcessor;
    }

    @Bean
    public TaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10000); //This number of minimum threads should be changed according to reach the best performance
        executor.setMaxPoolSize(Integer.MAX_VALUE);
        executor.setQueueCapacity(Integer.MAX_VALUE);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setThreadNamePrefix("MultiThreaded");
        return executor;
    }

    @Bean
    public ItemProcessor<AccountInfo, AccountInfo> itemProcessor() {
        ReceitaService receitaService = new ReceitaService();
        return (accountInfo) -> {
            updateAccount(receitaService, accountInfo);
            return accountInfo;
        };
    }

    private void updateAccount(ReceitaService receitaService, AccountInfo accountInfo) {
        try{
            boolean resultado = receitaService.atualizarConta(
                    accountInfo.getAgencia(),
                    accountInfo.getConta().replace("-", ""),
                    Double.parseDouble(accountInfo.getSaldo().replace(",", ".")),
                    accountInfo.getStatus()
            );
            accountInfo.setResultado(resultado);
        }
        catch (Exception e){
            // TODO: Check if Spring Batch has a native way to run the processor again in case of error in external services
            log.error("Error in receitaService, running the service again for account {}", accountInfo.getConta());
            updateAccount(receitaService, accountInfo);
        }
    }

    @Bean
    @StepScope
    public AsyncItemWriter<AccountInfo> asyncWriter() {
        AsyncItemWriter<AccountInfo> asyncItemWriter = new AsyncItemWriter<>();
        asyncItemWriter.setDelegate(itemWriter());
        return asyncItemWriter;
    }

    private FlatFileItemWriter<AccountInfo> itemWriter() {
        String fileName = new SimpleDateFormat("yyyyMMddHHmm'.csv'").format(new Date());
        return new FlatFileItemWriterBuilder<AccountInfo>()
                .name("Writer")
                .headerCallback(writer -> writer.write("agencia;conta;saldo;status;resultado"))
                .append(false)
                .resource(new FileSystemResource(fileName))
                .lineAggregator(new DelimitedLineAggregator<AccountInfo>() {
                    {
                        setDelimiter(";");
                        setFieldExtractor(new BeanWrapperFieldExtractor<AccountInfo>() {
                            {
                                setNames(new String[]{"agencia", "conta", "saldo", "status", "resultado"});
                            }
                        });
                    }
                })
                .build();

    }


}
