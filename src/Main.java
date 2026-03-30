import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicLongArray;
import java.util.concurrent.CountDownLatch;

public class Main {
        void main() throws InterruptedException {
                int threadCount = 5;
                int calculationLength = 50;

                AtomicIntegerArray progress = new AtomicIntegerArray(threadCount);
                AtomicLongArray times = new AtomicLongArray(threadCount);
                AtomicLongArray threadIds = new AtomicLongArray(threadCount);

                CountDownLatch finishLatch = new CountDownLatch(threadCount);

                ExecutorService executor = Executors.newFixedThreadPool(threadCount);

                System.out.println("Запуск многопоточного расчёта");

                for (int i = 0; i < threadCount; i++) {
                        System.out.println();
                }

                for (int i = 0; i < threadCount; i++) {
                        final int index = i;
                        executor.submit(() -> {
                                threadIds.set(index, Thread.currentThread().getId());
                                long startTime = System.currentTimeMillis();

                                for (int step = 0; step <= calculationLength; step++) {
                                        progress.set(index, step);

                                        try {
                                                TimeUnit.MILLISECONDS.sleep((long) (Math.random() * 80 + 20));
                                        } catch (InterruptedException e) {
                                                Thread.currentThread().interrupt();
                                        }
                                }

                                times.set(index, System.currentTimeMillis() - startTime);

                                finishLatch.countDown();
                        });
                }

                boolean isRendering = true;
                while (isRendering) {
                        if (finishLatch.getCount() == 0) {
                                isRendering = false;
                        }
                        StringBuilder output = new StringBuilder();

                        output.append("\033[").append(threadCount).append("A");

                        for (int i = 0; i < threadCount; i++) {
                                int p = progress.get(i);
                                double percentage = (double) p / calculationLength;

                                int barLength = 25;
                                int filled = (int) (percentage * barLength);

                                StringBuilder bar = new StringBuilder("[");
                                for (int j = 0; j < barLength; j++) {
                                        if (j < filled)
                                                bar.append("=");
                                        else if (j == filled && p < calculationLength)
                                                bar.append(">");
                                        else
                                                bar.append(" ");
                                }
                                bar.append("]");

                                long time = times.get(i);
                                String timeStr = time > 0 ? String.format(" | Время: %d мс", time) : "";
                                long id = threadIds.get(i);

                                output.append("\033[2K\r");
                                output.append(String.format("Поток #%-2d (ID %-3d): %s %5.1f%%%s\n",
                                        (i + 1), id == 0 ? -1 : id, bar.toString(), percentage * 100, timeStr));
                        }

                        System.out.print(output.toString());

                        try {
                                TimeUnit.MILLISECONDS.sleep(30);
                        } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                        }
                }

                executor.shutdown();
                executor.awaitTermination(1, TimeUnit.SECONDS);

                System.out.println("Расчёт завершён!");
        }
}