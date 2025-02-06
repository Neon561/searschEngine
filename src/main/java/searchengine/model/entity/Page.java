package searchengine.model.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "page")
@Data
public class Page {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) //
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id", nullable = false)
    private Site site;

    @Column(name = "path", nullable = false)
    private String path;

    @Column(name = "code", nullable = false)
    private Integer statusCode;

    @Column(name = "content")
    private String content;

}
