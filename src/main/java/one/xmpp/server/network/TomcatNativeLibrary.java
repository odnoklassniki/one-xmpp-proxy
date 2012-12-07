package one.xmpp.server.network;

import org.apache.tomcat.jni.Library;
import org.apache.tomcat.jni.SSL;

import one.ejb.NotNullByDefault;

@NotNullByDefault
public class TomcatNativeLibrary {

    private static boolean isLoaded;

    public static synchronized void load() {
        if (isLoaded) {
            return;
        }

        try {
            boolean loaded = false;

            if (!loaded) {
                loaded = tryLoad("tcnative-1");
            }

            if (!loaded) {
                loaded = tryLoad("libtcnative-1");
            }

            if (!loaded) {
                throw new UnsatisfiedLinkError("no tcnative-1 or libtcnative-1 in java.library.path");
            }

            Library.initialize(null);
            SSL.initialize(null);

            System.out.println("Apache Portable Runtime " + Library.aprVersionString() + " loaded");

        } catch (Exception exc) {
            throw new ExceptionInInitializerError(exc);
        }

        isLoaded = true;
    }

    private static boolean tryLoad(final String libname) {
        try {
            Runtime.getRuntime().loadLibrary(libname);
            return true;
        } catch (UnsatisfiedLinkError exc) {
            System.out.println("APR using library name '" + libname + "' not loaded: " + exc.getMessage());
        }
        return false;
    }
}
