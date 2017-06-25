package org.araymond.joal.web.messages.outgoing.impl.announce;

import org.araymond.joal.core.ttorent.client.MockedTorrent;
import org.araymond.joal.web.messages.outgoing.OutgoingMessageTypes;
import org.junit.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Created by raymo on 25/06/2017.
 */
public class AnnouncerHasFailedToAnnounceMessageTest {

    @Test
    public void shouldNotBuildWithoutTorrent() {
        assertThatThrownBy(() -> new AnnouncerHasFailedToAnnounceMessage(null, "this is an error"))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("Torrent must not be null.");
    }

    @Test
    public void shouldNotBuildWithoutMessage() {
        assertThatThrownBy(() -> new AnnouncerHasFailedToAnnounceMessage(Mockito.mock(MockedTorrent.class), " "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Error message must not be null or empty.");
    }

    @Test
    public void shouldBuild() {
        final MockedTorrent torrent = Mockito.mock(MockedTorrent.class);
        final AnnouncerHasFailedToAnnounceMessage message = new AnnouncerHasFailedToAnnounceMessage(torrent, "this is an error");

        assertThat(message.getType()).isEqualTo(OutgoingMessageTypes.ANNOUNCER_HAS_FAILED_TO_ANNOUNCE);
        assertThat(message.getTorrent()).isEqualTo(torrent);
        assertThat(message.getError()).isEqualTo("this is an error");
    }


}
