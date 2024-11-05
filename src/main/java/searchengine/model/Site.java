package searchengine.model;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "site")
@Getter
@Setter
public class Site {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    // todo - @NotNull //что за com.sun.istack ??? Должна быть аноташа про запрет на нул в джава ПРОВЕРИТЬ!
    //@NotNull
//    @Column(name = "status", nullable = false, columnDefinition = "ENUM('INDEXING', 'INDEXED', 'FAILED')")
    @Column(name = "status", nullable = false, columnDefinition = "VARCHAR(10)")
    private IndexingStatus status;

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
