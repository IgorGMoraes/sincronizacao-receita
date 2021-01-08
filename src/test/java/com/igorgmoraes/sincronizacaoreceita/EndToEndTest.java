package com.igorgmoraes.sincronizacaoreceita;

import com.igorgmoraes.sincronizacaoreceita.config.SpringBatchConfiguration;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.core.*;
import org.springframework.batch.test.AssertFile;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.JobRepositoryTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;

import java.util.Collection;

@RunWith(SpringRunner.class)
@SpringBatchTest
@EnableAutoConfiguration
@ContextConfiguration(classes = { SpringBatchConfiguration.class })
@TestExecutionListeners(DependencyInjectionTestExecutionListener.class)
public class EndToEndTest {

	private static final String TEST_OUTPUT = "src/test/resources/actual-output.csv";

	private static final String EXPECTED_OUTPUT = "src/test/resources/expected-output.json";

	private static final String TEST_INPUT = "src/test/resources/input.csv";

	@Autowired
	private JobLauncherTestUtils jobLauncherTestUtils;

	@Autowired
	private JobRepositoryTestUtils jobRepositoryTestUtils;

	@After
	public void cleanUp() {
		jobRepositoryTestUtils.removeJobExecutions();
	}

	private JobParameters defaultJobParameters() {
		JobParametersBuilder paramsBuilder = new JobParametersBuilder();
		paramsBuilder.addString("file.input", TEST_INPUT);
		paramsBuilder.addString("file.output", TEST_OUTPUT);
		return paramsBuilder.toJobParameters();
	}

	//TODO: Fix tests accountInfoItemReader(null) in SpringBatchConfiguration so tests can work
	@Test
	public void givenReferenceOutput_whenJobExecuted_thenSuccess() throws Exception {
		FileSystemResource expectedResult = new FileSystemResource(EXPECTED_OUTPUT);
		FileSystemResource actualResult = new FileSystemResource(TEST_OUTPUT);

		// when
		JobExecution jobExecution = jobLauncherTestUtils.launchJob(defaultJobParameters());
		JobInstance actualJobInstance = jobExecution.getJobInstance();
		ExitStatus actualJobExitStatus = jobExecution.getExitStatus();

		// then
		assertEquals("transformBooksRecords", actualJobInstance.getJobName());
		assertEquals("COMPLETED", actualJobExitStatus.getExitCode());

		AssertFile.assertFileEquals(expectedResult, actualResult);
	}

	@Test
	public void givenReferenceOutput_whenStepExecuted_thenSuccess() throws Exception {
		FileSystemResource expectedResult = new FileSystemResource(EXPECTED_OUTPUT);
		FileSystemResource actualResult = new FileSystemResource(TEST_OUTPUT);

		// when
		JobExecution jobExecution = jobLauncherTestUtils.launchStep("Synchronize file", defaultJobParameters());
		Collection<StepExecution> actualStepExecutions = jobExecution.getStepExecutions();
		ExitStatus actualJobExitStatus = jobExecution.getExitStatus();

		// then
		assertEquals(1, actualStepExecutions.size());
		assertEquals("COMPLETED", actualJobExitStatus.getExitCode());
		AssertFile.assertFileEquals(expectedResult, actualResult);
	}

}