package one.xmpp.utils;

import one.ejb.NotNullByDefault;

@NotNullByDefault
public class ExceptionUtils {

    /*
     * See
     * http://www.gamlor.info/wordpress/2010/02/throwing-checked-excpetions-like-unchecked-exceptions
     * -in-java/ for explanations
     */

    /**
     * Throw without wrapping or additional compiler "throws" declarations.
     */
    public static RuntimeException throwUnchecked(final Throwable ex) {
        return ExceptionUtils.<RuntimeException> throwUncheckedImpl(ex);
    }

    @SuppressWarnings("unchecked")
    private static <T extends Exception> T throwUncheckedImpl(Throwable toThrow) throws T {
        throw (T) toThrow;
    }

}
