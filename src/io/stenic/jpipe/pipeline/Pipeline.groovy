package io.stenic.jpipe.pipeline

import io.stenic.jpipe.plugin.PluginManager
import io.stenic.jpipe.event.EventDispatcher
import io.stenic.jpipe.event.Event

class Pipeline implements Serializable {
    protected final def script
    private PluginManager pluginManager = new PluginManager()
    private EventDispatcher eventDispatcher = new EventDispatcher()

    Pipeline(script) {
        this.script = script;
    }

    public void addPlugins(List plugins) {
        plugins.each { this.pluginManager.register(it) }
    }

    public void run(Map config, env) {
        this.pluginManager.getPlugins().each { this.eventDispatcher.addSubscriber(it) }

        def Event event = new Event()
        
        event.env = env;
        event.version = env.BUILD_ID;
        event.setScript(this.script)

        this.script.stage(Event.PREPARE) {
            this.eventDispatcher.dispatch(Event.PREPARE, event)
            this.script.currentBuild.displayName = event.version
        }

        this.script.stage(Event.BUILD) {
            this.script.echo "Version: ${event.version}";
            this.eventDispatcher.dispatch(Event.BUILD, event)
        }

        if (this.eventDispatcher.getListeners(Event.TEST).size() > 0) {
            this.script.stage(Event.TEST) {
                this.eventDispatcher.dispatch(Event.TEST, event)
            }
        }
        
        if (this.eventDispatcher.getListeners(Event.PUBLISH).size() > 0) {
            this.script.stage(Event.PUBLISH) {
                this.eventDispatcher.dispatch(Event.PUBLISH, event)
            }
        }
        
        if (this.eventDispatcher.getListeners(Event.DEPLOY).size() > 0) {
            this.script.stage(Event.DEPLOY) {
                this.eventDispatcher.dispatch(Event.DEPLOY, event)
            }
        }
        
        return 
    }
}
