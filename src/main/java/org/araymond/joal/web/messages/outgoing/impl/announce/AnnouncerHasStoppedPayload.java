package org.araymond.joal.web.messages.outgoing.impl.announce;

import org.araymond.joal.core.events.announce.AnnouncerHasStoppedEvent;

/**
 * Created by raymo on 25/06/2017.
 */
public class AnnouncerHasStoppedPayload extends AnnouncePayload {

    public AnnouncerHasStoppedPayload(final AnnouncerHasStoppedEvent event) {
        super(event);
    }
}
