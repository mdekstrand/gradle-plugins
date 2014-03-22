package net.elehack.gradle.science;

import org.slf4j.Logger;

enum MessageLevel {
    WARN {
        void print(Logger log, String msg) {
            log.warn(msg);
        }
    },
    ERROR {
        void print(Logger log, String msg) {
            log.error(msg);
        }
    },
    INFO {
        void print(Logger log, String msg) {
            log.info(msg);
        }
    };
    
    abstract void print(Logger log, String msg);
}
