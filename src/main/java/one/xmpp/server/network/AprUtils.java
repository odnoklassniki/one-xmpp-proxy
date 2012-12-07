package one.xmpp.server.network;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.tomcat.jni.Error;
import org.apache.tomcat.jni.Poll;
import org.apache.tomcat.jni.Socket;
import org.apache.tomcat.jni.Status;

import one.ejb.NotNullByDefault;

@NotNullByDefault
class AprUtils {

    private static final Log log = LogFactory.getLog(AprUtils.class);

    public static void closeAndDestroySocketSafe(long socketPointer) {
        try {
            try {
                if (log.isTraceEnabled()) {
                    log.trace("Closing socket #" + socketPointer);
                }
                int result = Socket.close(socketPointer);
                if (result != Status.APR_SUCCESS) {
                    log.error("Unable to close socket #" + socketPointer + ": Error #" + result + ": "
                            + Error.strerror(result));
                } else {
                    if (log.isTraceEnabled()) {
                        log.trace("Closed socket #" + socketPointer);
                    }
                }
            } catch (Throwable exc) {
                log.error("Unable to close socket #" + socketPointer + ": " + exc);
            }

            if (log.isTraceEnabled()) {
                log.trace("Destroying socket #" + socketPointer);
            }
            Socket.destroy(socketPointer);
            if (log.isTraceEnabled()) {
                log.trace("Destroyed socket #" + socketPointer);
            }
        } catch (Throwable exc) {
            log.error("Unable to close and/or destroy socket #" + socketPointer + ": " + exc);
        }
    }

    public static void removeFromPollSafe(final long pollPointer, final long clientSocketPointer) {
        try {
            int result = Poll.remove(pollPointer, clientSocketPointer);

            if (log.isTraceEnabled()) {
                log.trace("Removed socket #" + clientSocketPointer + " from poll #" + pollPointer + " with result "
                        + result);
            }

        } catch (Throwable exc) {
            log.error("Unable to remove from poll #" + pollPointer + " socket #" + clientSocketPointer + ": " + exc,
                    exc);
        }
    }

}
