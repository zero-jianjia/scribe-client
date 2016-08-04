package com.zero.scribe.log4j2plugin;

import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.appender.AppenderLoggingException;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.Required;

import java.io.Serializable;

/**
 * Created by jianjia1 on 16/07/28.
 */
@Plugin(name = "Scribe", category = Node.CATEGORY,
        elementType = Appender.ELEMENT_TYPE, printObject = true)
public final class ScribeAppender extends AbstractAppender {
    private final ScribeManager manager;

    @PluginFactory
    public static ScribeAppender createAppender(
            @PluginElement("Layout") final Layout<? extends Serializable> layout,
            @PluginElement("Filter") final Filter filter,
            @Required(message = "No name provided for ScribeAppender") @PluginAttribute("name") final String name,
            @PluginAttribute(value = "ignoreExceptions", defaultBoolean = true) final boolean ignoreExceptions,
            @Required(message = "No host provided for ScribeAppender") @PluginAttribute("host") final String host,
            @Required(message = "No category provided for ScribeAppender") @PluginAttribute("category") final String category,
            @PluginAttribute("fileName") final String fileName,
            @PluginAttribute("batchSize") final int batchSize) {
        final ScribeManager scribeManager = new ScribeManager(name, host, category, fileName, batchSize);
        return new ScribeAppender(name, layout, filter, ignoreExceptions, scribeManager);
    }

    private ScribeAppender(final String name, final Layout<? extends Serializable> layout,
            final Filter filter, final boolean ignoreExceptions, final ScribeManager manager) {
        super(name, filter, layout, ignoreExceptions);
        this.manager = manager;
    }

    @Override
    public void append(final LogEvent event) {
        try {
            //StringEncoder.toBytes(event.getMessage().getFormattedMessage(),
            //                      StandardCharsets.UTF_8);
            //manager.append(event.getMessage().getFormattedMessage());
            boolean appendOK = false;
            for (int i = 0; i < 3 && !appendOK; i++) {
                appendOK = manager.append(getLayout().toByteArray(event));
            }
            if (!appendOK)
                throw new Exception("BlockingQueue Is Full.");
        } catch (InterruptedException e) {
            LOGGER.error("Unable to write to Scribe for appender [{}].", getName(), e);
            throw new AppenderLoggingException("Unable to write to Scribe in appender: " + e.getMessage(), e);
        } catch (Exception ex) {
            LOGGER.error("Unable to write to Scribe for appender [{}].", getName(), ex);
            throw new AppenderLoggingException("Unable to write to Scribe in appender: " + ex.getMessage(), ex);
        }
    }

    @Override
    public void start() {
        super.start();
        manager.startup();
    }

    @Override
    public void stop() {
        super.stop();
        manager.release();
    }

    @Override
    public String toString() {
        return "ScribeAppender{" +
                "name=" + getName() +
                ", state=" + getState() +
                '}';
    }
}
