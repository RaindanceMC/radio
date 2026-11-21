package de.raindancer.modules.radio;

import de.raindancer.core.ui.chat.Brand;
import de.raindancer.core.ui.messages.Messages;
import de.raindancer.modules.radio.service.RadioService;

/** What this module built, handed to its command and its page. */
public record RadioServices(Messages messages, Brand brand, RadioService radio) {
}
