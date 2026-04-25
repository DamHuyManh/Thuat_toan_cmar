package cmar.util;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;

/**
 * Sampler đo peak heap memory trong lúc chạy một phase.
 * Dùng thread nền sample mỗi 20ms.
 */
public class MemorySampler {
    private final MemoryMXBean bean = ManagementFactory.getMemoryMXBean();
    private volatile long peakBytes = 0;
    private volatile long baselineBytes = 0;
    private Thread sampler;
    private volatile boolean running = false;
    private final long intervalMs;

    public MemorySampler() { this(20); }

    public MemorySampler(long intervalMs) { this.intervalMs = intervalMs; }

    public void start() {
        System.gc();
        try { Thread.sleep(50); } catch (InterruptedException ignored) {}
        baselineBytes = bean.getHeapMemoryUsage().getUsed();
        peakBytes = baselineBytes;
        running = true;
        sampler = new Thread(() -> {
            while (running) {
                long u = bean.getHeapMemoryUsage().getUsed();
                if (u > peakBytes) peakBytes = u;
                try { Thread.sleep(intervalMs); } catch (InterruptedException e) { break; }
            }
        }, "mem-sampler");
        sampler.setDaemon(true);
        sampler.start();
    }

    public void stop() {
        running = false;
        if (sampler != null) {
            try { sampler.join(100); } catch (InterruptedException ignored) {}
        }
    }

    public long peakBytes() { return peakBytes; }
    public long peakMB() { return peakBytes / (1024 * 1024); }
    public long deltaBytes() { return Math.max(0, peakBytes - baselineBytes); }
    public long deltaMB() { return deltaBytes() / (1024 * 1024); }
}
