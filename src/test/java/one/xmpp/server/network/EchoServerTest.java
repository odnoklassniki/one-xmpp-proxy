package one.xmpp.server.network;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.net.SocketFactory;

import org.apache.commons.lang.StringUtils;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;

import one.xmpp.AbstractSpringTest;

public class EchoServerTest extends AbstractSpringTest {

    private static SocketFactory socketFactory;

    @BeforeClass
    public static void beforeClass() throws Exception {
        AbstractSpringTest.beforeClass(true, false, EchoServer.class);
        Thread.sleep(1000);

        socketFactory = SocketFactory.getDefault();
    }

    private static void test() throws Exception {
        final Socket socket = socketFactory.createSocket("localhost", 5222);
        try {

            final OutputStream outputStream = socket.getOutputStream();
            try {
                final InputStream inputStream = socket.getInputStream();

                try {
                    // send "hi!"
                    outputStream.write("Hello!".getBytes());

                    byte[] buffer = new byte[100];

                    final long start = System.currentTimeMillis();
                    int read = 0;
                    while (read < 6 && (System.currentTimeMillis() - start) < 10000) {
                        read += inputStream.read(buffer, read, buffer.length - read);
                    }
                } finally {
                    inputStream.close();
                }
            } finally {
                outputStream.close();
            }
        } finally {
            socket.close();
        }
    }

    private static void testLongImpl() throws Exception {
        final Socket socket = socketFactory.createSocket("localhost", 5222);
        try {

            final OutputStream outputStream = socket.getOutputStream();
            try {
                final InputStream inputStream = socket.getInputStream();

                try {
                    // 2 Mb
                    final byte[] source = StringUtils.repeat("01234567", 1 << 7 << 11).getBytes();

                    final CountDownLatch countDownLatch = new CountDownLatch(1);

                    new Thread() {
                        @Override
                        public void run() {
                            // send "hi!"
                            countDownLatch.countDown();
                            try {
                                outputStream.write(source);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        };
                    }.start();

                    final AtomicInteger read = new AtomicInteger(0);
                    new Thread() {
                        @Override
                        public void run() {
                            try {
                                countDownLatch.await();
                                Thread.sleep(5000);
                                byte[] buffer = new byte[source.length + 1];
                                while (read.get() < source.length) {
                                    read.addAndGet(inputStream.read(buffer, read.get(), source.length - read.get()));
                                }
                            } catch (Exception exc) {
                                exc.printStackTrace();
                            }
                        };
                    }.start();

                    countDownLatch.await();
                    final long start = System.currentTimeMillis();
                    while (read.get() < source.length && (System.currentTimeMillis() - start) < 10000) {
                        Thread.sleep(100);
                    }

                    System.out.println("Total passed: " + read.get() + " of  " + source.length);
                    Assert.assertEquals(source.length, read.get());
                } finally {
                    inputStream.close();
                }
            } finally {
                outputStream.close();
            }
        } finally {
            socket.close();
        }
    }

    @Test
    public void testConnect() throws Exception {
        test();
    }

    @Test
    public void testLong() throws Exception {
        testLongImpl();
    }

    @Test
    public void testMultiple() throws Exception {
        for (int i = 0; i < 1000; i++) {
            test();
        }
    }

    @Test
    public void testMultithread() throws Exception {
        ExecutorService executorService = Executors.newFixedThreadPool(10, new CustomizableThreadFactory("Test-") {

            private static final long serialVersionUID = 1L;

            @Override
            public Thread createThread(Runnable runnable) {
                Thread thread = super.createThread(runnable);
                thread.setPriority(Thread.MIN_PRIORITY);
                return thread;
            }
        });

        new Thread() {
            {
                setDaemon(true);
                setPriority(MIN_PRIORITY);
            }

            @Override
            public void run() {
                while (true) {
                    System.gc();
                    Runtime.getRuntime().runFinalization();
                }
            }
        }.start();

        final int count = 10000;
        List<Future<?>> futures = new ArrayList<Future<?>>(count);
        for (int i = 0; i < count; i++) {
            futures.add(executorService.submit(new Callable<Object>() {
                @Override
                public Object call() throws Exception {
                    test();
                    return null;
                }
            }));
        }

        for (Future<?> future : futures) {
            future.get();
        }

        executorService.shutdown();
        executorService.awaitTermination(1, TimeUnit.MINUTES);
    }
}
