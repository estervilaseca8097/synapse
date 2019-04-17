package de.otto.synapse.edison.journal;

import de.otto.synapse.journal.Journal;
import de.otto.synapse.journal.Journals;
import de.otto.synapse.message.TextMessage;
import de.otto.synapse.messagestore.MessageStoreEntry;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import java.util.Optional;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@RunWith(SpringRunner.class)
@ActiveProfiles("test")
@ContextConfiguration(
        classes = JournalUiController.class)
@WebMvcTest(
        controllers = JournalUiController.class,
        secure = false)
@EnableAutoConfiguration
@ComponentScan(
        basePackages = {"de.otto.synapse.edison.state","de.otto.synapse.edison.journal"})
public class JournalUiControllerTest {

    @Autowired
    private WebApplicationContext context;

    @MockBean
    private Journals journals;

    private MockMvc mockMvc;

    @Before
    public void setup() {
        mockMvc = webAppContextSetup(context).build();
    }

    @Test
    public void shouldConfigureJournalUiController() {
        assertThat(context.containsBean("journalUiController"), is(true));
    }

    @Test
    public void shouldGetEmptyJournalHtml() throws Exception {
        final Journal journal = mock(Journal.class);
        when(journal.getJournalFor("first")).thenReturn(Stream.of());

        when(journals.containsKey("test")).thenReturn(true);
        when(journals.getJournal("test")).thenReturn(Optional.of(journal));

        mockMvc
                .perform(
                        get("/internal/journals/test/first").accept("text/html"))
                .andExpect(
                        status().isOk())
                .andExpect(
                        model().attribute("title", is("Journal: test/first")))
                .andExpect(
                        model().attribute("messages", is(emptyList())));
    }

    @Test
    public void shouldGetJournalHtml() throws Exception {
        final Journal journal = mock(Journal.class);
        when(journal.getJournalFor("first"))
                .thenReturn(Stream.of(
                        MessageStoreEntry.of("test", TextMessage.of("first", "foo")),
                        MessageStoreEntry.of("test", TextMessage.of("first", "bar"))
                ));

        when(journals.containsKey("test")).thenReturn(true);
        when(journals.getJournal("test")).thenReturn(Optional.of(journal));

        mockMvc
                .perform(
                        get("/internal/journals/test/first").accept("text/html"))
                .andExpect(
                        status().isOk())
                .andExpect(
                        model().attribute("title", is("Journal: test/first")))
                .andExpect(
                        model().attribute("messages", hasSize(2)))
                .andExpect(
                        content().string(containsString("<td class=\"col-md-1\">first</td>")));
    }

    @Test
    public void shouldGet404ForMissingJournalHtml() throws Exception {
        when(journals.containsKey("test")).thenReturn(false);

        mockMvc
                .perform(
                        get("/internal/journals/test/first").accept("text/html"))
                .andExpect(
                        status().isNotFound());
    }


}