package org.osbo.bots;

import org.junit.jupiter.api.Test;
import org.osbo.bots.crons.DeleteVencidos;
import org.osbo.bots.crons.Horarios;
import org.osbo.bots.runner.StartupRunner;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(properties = "spring.datasource.url=jdbc:sqlite:file:application-test?mode=memory&cache=shared")
class DatesBotApplicationTests {

	@MockitoBean
	private StartupRunner startupRunner;

	@MockitoBean
	private DeleteVencidos deleteVencidos;

	@MockitoBean
	private Horarios horarios;

	@Test
	void contextLoads() {
	}

}
