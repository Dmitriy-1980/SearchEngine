package searchengine.model;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "site")
@Getter
@Setter
public class SiteEntity {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "status", nullable = false, columnDefinition = "VARCHAR(10)")
    private String status;

    @Column(name = "status_time", nullable = false, columnDefinition = "DATETIME")
    private LocalDateTime statusTime;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @Column(name = "url", columnDefinition = "VARCHAR(255) NOT NULL")
    private String url;


    @Column(name = "name", nullable = false, columnDefinition = "VARCHAR(255)")
    private String name;


    @Override
    public String toString() {
        return "Site{ id:" + id + " ; url: " + url + " ; name: " + name + " ; status: " + status + " ; statusTime: " +
                statusTime + " ; url: " + url + "}";
    }
}
