package org.araymond.joal.core.ttorent.client.bandwidth;

import com.google.common.base.Preconditions;
import com.turn.ttorrent.common.protocol.TrackerMessage;
import org.araymond.joal.core.config.JoalConfigProvider;
import org.araymond.joal.core.ttorent.client.announce.Announcer;
import org.araymond.joal.core.ttorent.client.announce.AnnouncerEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Created by raymo on 14/05/2017.
 */
public class BandwidthDispatcher implements AnnouncerEventListener, Runnable {

    private final JoalConfigProvider configProvider;
    /**
     * Update interval have to be a low value, because when a torrent is added, the Thread.pause may end and split value
     * among all torrent and add a non reasonable value to the freshly added torrent
     */
    private final Integer updateInterval;
    private final List<TorrentWithStats> torrents;
    private final ReentrantReadWriteLock lock;

    private Thread thread;
    private boolean stop;

    public BandwidthDispatcher(final JoalConfigProvider configProvider) {
        this(configProvider, 1000);
    }

    BandwidthDispatcher(final JoalConfigProvider configProvider, final Integer updateInterval) {
        Preconditions.checkNotNull(configProvider, "Cannot build without ConfigProvider.");
        this.configProvider = configProvider;
        this.updateInterval = updateInterval;
        this.torrents = new ArrayList<>(configProvider.get().getSimultaneousSeed());
        lock = new ReentrantReadWriteLock();
    }

    @Override
    public void onAnnouncerWillAnnounce(final TrackerMessage.AnnounceRequestMessage.RequestEvent event, final Announcer announcer) {
    }

    @Override
    public void onAnnounceSuccess(final Announcer announcer) {
        // compute speed on announce success, because we need to access leechers and seeders count
        final TorrentWithStats torrent = announcer.getSeedingTorrent();
        // if there is no leechers or seeders the speed is set to 0;
        if (torrent.getLeechers() == 0 || torrent.getSeeders() == 0) {
            torrent.refreshRandomSpeedInBytes(0L);
            return;
        }

        final Long minUploadRateInBytes = configProvider.get().getMinUploadRate() * 1000L;
        final Long maxUploadRateInBytes = configProvider.get().getMaxUploadRate() * 1000L;
        final Long randomSpeedInBytes = (minUploadRateInBytes.equals(maxUploadRateInBytes))
                ? maxUploadRateInBytes
                : ThreadLocalRandom.current().nextLong(minUploadRateInBytes, maxUploadRateInBytes);

        this.lock.writeLock().lock();
        announcer.getSeedingTorrent().refreshRandomSpeedInBytes(randomSpeedInBytes);
        this.lock.writeLock().unlock();
    }

    @Override
    public void onAnnounceFail(final Announcer announcer, final String errMessage) {
    }

    @Override
    public void onNoMoreLeecherForTorrent(final Announcer announcer, final TorrentWithStats torrent) {
    }

    @Override
    public void onShouldDeleteTorrent(final Announcer announcer, final TorrentWithStats torrent) {
    }

    @Override
    public void onAnnouncerStart(final Announcer announcer) {
        this.lock.writeLock().lock();
        try {
            this.torrents.add(announcer.getSeedingTorrent());
        } finally {
            this.lock.writeLock().unlock();
        }
    }

    @Override
    public void onAnnouncerStop(final Announcer announcer) {
        this.lock.writeLock().lock();
        try {
            this.torrents.remove(announcer.getSeedingTorrent());
        } finally {
            this.lock.writeLock().unlock();
        }
    }

    public int getTorrentCount() {
        return this.torrents.size();
    }

    public void start() {
        this.stop = false;
        if (this.thread == null || !this.thread.isAlive()) {
            this.thread = new Thread(this);
            this.thread.setName("bandwidth-manager");
            this.thread.start();
        }
    }

    public void stop() {
        this.stop = true;
        if (this.thread != null) {
            this.thread.interrupt();

            try {
                this.thread.join();
            } catch (final InterruptedException ignored) {
            }
            this.thread = null;

            this.lock.writeLock().lock();
            try {
                this.torrents.clear();
            } finally {
                this.lock.writeLock().unlock();
            }
        }
    }

    @Override
    public void run() {
        try {
            while (!this.stop) {
                Thread.sleep(updateInterval);

                lock.readLock().lock();

                final long torrentCount = this.torrents.stream()
                        .filter(torrent -> torrent.getCurrentRandomSpeedInBytes() > 0)
                        .count();

                // prevent divide by 0
                if (torrentCount != 0) {
                    for (final TorrentWithStats torrent : this.torrents) {
                        final long uploadRateInBytesForTorrent = torrent.getCurrentRandomSpeedInBytes() / torrentCount;

                        torrent.addUploaded(uploadRateInBytesForTorrent * updateInterval / 1000);
                    }
                }
                lock.readLock().unlock();
            }
        } catch (final InterruptedException ignored) {
        } finally {
            if (this.lock.getReadLockCount() > 0) {
                lock.readLock().unlock();
            }
        }
    }

}
