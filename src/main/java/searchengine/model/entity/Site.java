package searchengine.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.UpdateTimestamp;
import searchengine.model.SiteStatus;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Entity
@Table(name = "site")
@Data
public class Site {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private SiteStatus siteStatus;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @Column(name = "url", nullable = false)
    private String url;

    @Column(name = "name", nullable = false)
    private String siteName;

    @UpdateTimestamp
    @Column(name = "status_time")
    private Instant statusTime;


    @OneToMany(mappedBy = "site", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<Page> pages;

    @Transient // Не сохраняем в БД
    private final AtomicInteger activeTasks = new AtomicInteger(0);

    public void incrementTasks() {
        activeTasks.incrementAndGet();
    }

    public boolean decrementTasks() {
        int remainingTasks = activeTasks.decrementAndGet();
        if (remainingTasks == 0) {
            setSiteStatus(SiteStatus.INDEXED);
            setStatusTime(Instant.now());
            System.out.println("Сайт " + url + " полностью проиндексирован.");
            return true;
        }
        return false;
    }

}
