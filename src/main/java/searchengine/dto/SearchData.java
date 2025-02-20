package searchengine.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SearchData {
    private String site;       // URL сайта
    private String siteName;  // Название сайта
    private String uri;       // Путь к странице
    private String title;     // Заголовок страницы
    private String snippet;   // Сниппет
    private float relevance;  // Релевантность
}