package com.magiclibrary.controllers;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.magiclibrary.dto.item.ItemResponseDTO;
import com.magiclibrary.entities.Item;
import com.magiclibrary.exceptions.custom.ItemNotFoundException;
import com.magiclibrary.repositories.interfaces.ItemRepository;
import com.magiclibrary.services.ItemService;

/**
 * Contrôleur SSR de la bibliothèque numérique.
 *
 * Cette classe gère l'affichage du catalogue public, la consultation
 * de la fiche détaillée d'un objet ainsi que le moteur de suggestions
 * utilisé par la recherche dynamique.
 */
@Controller
public class BibliothequePageController {

    /*
     * Paramètres utilisés par le moteur de recherche et l'affichage
     * du catalogue numérique.
     */
    private static final int SUGGEST_LIMIT = 8;
    private static final int MAX_QUERY_LENGTH = 80;
    private static final int MIN_TOKEN_LENGTH = 2;
    private static final int DEFAULT_PAGE_SIZE = 9;

    /*
     * Liste des mots ignorés lors du découpage des requêtes afin
     * d'améliorer la pertinence des recherches.
     */
    private static final java.util.Set<String> STOP_WORDS = java.util.Set.of(
            "de", "du", "des", "d",
            "la", "le", "les", "l",
            "un", "une",
            "et", "ou",
            "a", "au", "aux",
            "the", "of", "and", "or"
    );

    private final ItemService itemService;
    private final ItemRepository itemRepository;

    public BibliothequePageController(ItemService itemService, ItemRepository itemRepository) {
        this.itemService = itemService;
        this.itemRepository = itemRepository;
    }

    /*
     * Redirige l'ancienne route bibliothèque vers l'URL canonique
     * utilisée par l'application.
     */
    @GetMapping("/bibliotheque")
    public String bibliothequeRedirectToCanonical() {
        return "redirect:/bibliotheque-numerique";
    }

    /*
     * Affiche la page principale de la bibliothèque numérique.
     *
     * La méthode gère la recherche, le tri et la pagination
     * des objets du catalogue numérique.
     */
    @GetMapping("/bibliotheque-numerique")
    public String bibliothequeNumeriquePage(
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "sort", required = false) String sort,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "9") int size,
            Model model
    ) {
        String normalizedSort = normalizeSort(sort);
        String normalizedQuery = normalizePageQuery(q);
        int safePage = Math.max(page, 0);
        int safeSize = size > 0 ? size : DEFAULT_PAGE_SIZE;

        Page<ItemResponseDTO> itemsPage = itemService.getItemsPage(normalizedQuery, normalizedSort, safePage, safeSize);

        if (safePage > 0 && itemsPage.getTotalPages() > 0 && safePage >= itemsPage.getTotalPages()) {
            safePage = itemsPage.getTotalPages() - 1;
            itemsPage = itemService.getItemsPage(normalizedQuery, normalizedSort, safePage, safeSize);
        }

        model.addAttribute("items", itemsPage.getContent());
        model.addAttribute("q", normalizedQuery);
        model.addAttribute("sort", normalizedSort);
        model.addAttribute("pageSize", safeSize);
        model.addAttribute("currentPage", itemsPage.getNumber());
        model.addAttribute("totalPages", itemsPage.getTotalPages());
        model.addAttribute("totalElements", itemsPage.getTotalElements());
        model.addAttribute("hasPrevious", itemsPage.hasPrevious());
        model.addAttribute("hasNext", itemsPage.hasNext());
        model.addAttribute("paginationEnabled", itemsPage.getTotalPages() > 1);
        model.addAttribute("activePage", "bibliotheque-numerique");

        return "bibliotheque-numerique";
    }

    /*
     * Affiche la fiche détaillée d'un objet du catalogue numérique.
     * Une réponse HTTP 404 est renvoyée si l'objet n'existe pas.
     */
    @GetMapping("/bibliotheque/item/{id}")
    public String bibliothequeItemPage(
            @PathVariable("id") Integer id,
            Model model,
            HttpServletResponse response
    ) {

        ItemResponseDTO item = null;

        try {
            item = itemService.getItemById(id);
        } catch (ItemNotFoundException ex) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        }

        model.addAttribute("item", item);
        return "bibliotheque-objet-detail";
    }

    /*
     * Fournit les suggestions de recherche utilisées par
     * l'autocomplétion de la bibliothèque numérique.
     */
    @GetMapping(value = "/bibliotheque/suggest", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<List<ItemSuggestResponse>> suggest(@RequestParam(name = "q", required = false) String q) {

        String query = normalizeQuery(q);

        if (query.isEmpty()) {
            return ResponseEntity.ok(List.of());
        }

        if (!thresholdReached(query)) {
            return ResponseEntity.ok(List.of());
        }

        if (containsForbiddenChars(query)) {
            return ResponseEntity.ok(List.of());
        }

        Map<Integer, ItemSuggestResponse> out = new LinkedHashMap<>(SUGGEST_LIMIT * 2);

        boolean numeric = isNumeric(query);

        if (numeric) {
            addNumericPrefixMatches(out, query);
        }

        if (out.size() < SUGGEST_LIMIT) {
            List<String> tokens = tokenize(query);

            if (!tokens.isEmpty()) {
                String seed = tokens.get(0);

                addAll(out, itemRepository.findTop8ByDeletedDateItemIsNullAndTitleItemContainingIgnoreCaseOrderByTitleItemAsc(seed));
                if (out.size() < SUGGEST_LIMIT) {
                    addAll(out, itemRepository.findTop8ByDeletedDateItemIsNullAndAuthorItemContainingIgnoreCaseOrderByAuthorItemAsc(seed));
                }
                if (out.size() < SUGGEST_LIMIT) {
                    addAll(out, itemRepository.findTop8ByDeletedDateItemIsNullAndPublisherItemContainingIgnoreCaseOrderByPublisherItemAsc(seed));
                }
                if (out.size() < SUGGEST_LIMIT) {
                    addAll(out, itemRepository.findTop8ByDeletedDateItemIsNullAndCategoryItemContainingIgnoreCaseOrderByCategoryItemAsc(seed));
                }
                if (out.size() < SUGGEST_LIMIT) {
                    addAll(out, itemRepository.findTop8ByDeletedDateItemIsNullAndIsbnItemContainingIgnoreCaseOrderByIsbnItemAsc(seed));
                }

                if (tokens.size() > 1 && !out.isEmpty()) {
                    filterRequireAllTokens(out, tokens);
                }
            }
        }

        return ResponseEntity.ok(new ArrayList<>(out.values()));
    }

    /*
     * Recherche les objets dont l'identifiant commence par la valeur
     * numérique saisie afin de faciliter la recherche par référence.
     */
    private void addNumericPrefixMatches(Map<Integer, ItemSuggestResponse> out, String query) {
        if (out == null || out.size() >= SUGGEST_LIMIT || query == null || query.isBlank()) {
            return;
        }

        String normalizedQuery = normalizeNumericToken(query);
        if (normalizedQuery.isEmpty()) {
            return;
        }

        List<Item> items = new ArrayList<>();
        for (Item item : itemRepository.findAll()) {
            if (item == null || item.getDeletedDateItem() != null || item.getIdItem() == null) {
                continue;
            }
            items.add(item);
        }

        items.sort(Comparator.comparing(Item::getIdItem));

        for (Item item : items) {
            if (out.size() >= SUGGEST_LIMIT) {
                return;
            }

            String itemId = normalizeNumericToken(String.valueOf(item.getIdItem()));
            if (itemId.startsWith(normalizedQuery)) {
                out.putIfAbsent(item.getIdItem(), toSuggest(item));
            }
        }
    }

    /*
     * Applique un filtrage complémentaire afin d'exiger la présence
     * de tous les termes significatifs de la recherche.
     */
    private static void filterRequireAllTokens(Map<Integer, ItemSuggestResponse> out, List<String> tokens) {
        if (out == null || out.isEmpty() || tokens == null || tokens.isEmpty()) {
            return;
        }

        List<Integer> toRemove = new ArrayList<>();

        for (Map.Entry<Integer, ItemSuggestResponse> e : out.entrySet()) {
            ItemSuggestResponse s = e.getValue();
            if (s == null) {
                toRemove.add(e.getKey());
                continue;
            }

            String haystack = buildHaystack(s);

            boolean ok = true;
            for (String t : tokens) {
                if (t == null || t.isEmpty()) {
                    continue;
                }
                if (!haystack.contains(normalizeTokenForMatch(t))) {
                    ok = false;
                    break;
                }
            }

            if (!ok) {
                toRemove.add(e.getKey());
            }
        }

        for (Integer id : toRemove) {
            out.remove(id);
        }
    }

    /*
     * Construit la chaîne de comparaison utilisée lors des recherches
     * multi-critères sur les suggestions.
     */
    private static String buildHaystack(ItemSuggestResponse s) {
        String id = s.getId() == null ? "" : normalizeNumericToken(String.valueOf(s.getId()));
        String title = normalizeText(s.getTitle());
        String category = normalizeText(s.getCategory());
        String author = normalizeText(s.getAuthor());
        String publisher = normalizeText(s.getPublisher());
        String isbn = normalizeText(s.getIsbn());
        String tags = normalizeText(s.getTags());

        return (id + " " + title + " " + category + " " + author + " " + publisher + " " + isbn + " " + tags).trim();
    }

    /*
     * Normalise une chaîne de caractères pour les comparaisons
     * en supprimant notamment les accents et caractères parasites.
     */
    private static String normalizeText(String v) {
        if (v == null || v.isBlank()) {
            return "";
        }

        String lower = v.toLowerCase(Locale.ROOT).trim();

        String normalized = Normalizer.normalize(lower, Normalizer.Form.NFD);
        normalized = normalized.replaceAll("\\p{M}+", "");

        normalized = normalized.replace(',', ' ');
        normalized = normalized.replace(';', ' ');
        normalized = normalized.replace(':', ' ');
        normalized = normalized.replaceAll("\\s+", " ").trim();

        return normalized;
    }

    /*
     * Découpe une requête utilisateur en termes exploitables
     * par le moteur de recherche.
     */
    private static List<String> tokenize(String raw) {
        if (raw == null) {
            return List.of();
        }

        String cleaned = normalizeText(raw);
        if (cleaned.isEmpty()) {
            return List.of();
        }

        String[] parts = cleaned.split(" ");
        List<String> tokens = new ArrayList<>(parts.length);

        for (String p : parts) {
            if (p == null) {
                continue;
            }
            String t = p.trim();
            if (t.isEmpty()) {
                continue;
            }
            if (STOP_WORDS.contains(t)) {
                continue;
            }
            if (isNumeric(t)) {
                tokens.add(normalizeNumericToken(t));
                continue;
            }
            if (t.length() < MIN_TOKEN_LENGTH) {
                continue;
            }
            tokens.add(t);
        }

        return tokens;
    }

    private static String normalizeQuery(String q) {
        if (q == null) {
            return "";
        }

        String trimmed = q.trim();
        if (trimmed.isEmpty()) {
            return "";
        }

        if (trimmed.length() > MAX_QUERY_LENGTH) {
            return trimmed.substring(0, MAX_QUERY_LENGTH).trim();
        }

        return trimmed;
    }

    private static String normalizePageQuery(String q) {
        return normalizeQuery(q);
    }

    /*
     * Normalise la valeur de tri afin de garantir un comportement
     * cohérent de la bibliothèque numérique.
     */
    private static String normalizeSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return "availabilityThenTitle";
        }
        return sort.trim();
    }

    /*
     * Détermine si la saisie est suffisamment longue pour lancer
     * une recherche de suggestions.
     */
    private static boolean thresholdReached(String q) {
        if (q == null || q.isEmpty()) {
            return false;
        }
        return isNumeric(q) ? q.length() >= 1 : q.length() >= 2;
    }

    /*
     * Rejette certains caractères afin d'éviter des requêtes
     * non pertinentes ou potentiellement problématiques.
     */
    private static boolean containsForbiddenChars(String q) {
        for (int i = 0; i < q.length(); i++) {
            char c = q.charAt(i);
            if (c == '/' || c == '\\') {
                return true;
            }
        }
        return q.contains("//");
    }

    private static void addAll(Map<Integer, ItemSuggestResponse> out, List<Item> items) {
        if (items == null || items.isEmpty()) {
            return;
        }

        for (Item i : items) {
            if (out.size() >= SUGGEST_LIMIT) {
                return;
            }
            if (i == null || i.getIdItem() == null) {
                continue;
            }
            out.putIfAbsent(i.getIdItem(), toSuggest(i));
        }
    }

    private static ItemSuggestResponse toSuggest(Item i) {
        return new ItemSuggestResponse(
                i.getIdItem(),
                i.getTitleItem(),
                i.getCategoryItem(),
                i.getAuthorItem(),
                i.getPublisherItem(),
                i.getIsbnItem(),
                i.getTagsItem()
        );
    }

    private static boolean isNumeric(String s) {
        if (s == null || s.isEmpty()) {
            return false;
        }
        for (int idx = 0; idx < s.length(); idx++) {
            char c = s.charAt(idx);
            if (c < '0' || c > '9') {
                return false;
            }
        }
        return true;
    }

    /*
     * Uniformise les valeurs numériques utilisées dans les comparaisons
     * afin d'ignorer les zéros non significatifs.
     */
    private static String normalizeNumericToken(String s) {
        if (s == null || s.isBlank()) {
            return "";
        }

        String trimmed = s.trim();
        if (!isNumeric(trimmed)) {
            return trimmed;
        }

        String normalized = trimmed.replaceFirst("^0+", "");
        return normalized.isEmpty() ? "0" : normalized;
    }

    private static String normalizeTokenForMatch(String token) {
        if (token == null || token.isBlank()) {
            return "";
        }

        String trimmed = token.trim();
        if (isNumeric(trimmed)) {
            return normalizeNumericToken(trimmed);
        }

        return normalizeText(trimmed);
    }

    /*
     * DTO interne utilisé uniquement pour exposer les suggestions
     * d'objets au format JSON.
     */
    public static final class ItemSuggestResponse {

        private Integer id;
        private String title;
        private String category;
        private String author;
        private String publisher;
        private String isbn;
        private String tags;

        public ItemSuggestResponse() {
        }

        public ItemSuggestResponse(
                Integer id,
                String title,
                String category,
                String author,
                String publisher,
                String isbn,
                String tags
        ) {
            this.id = id;
            this.title = title;
            this.category = category;
            this.author = author;
            this.publisher = publisher;
            this.isbn = isbn;
            this.tags = tags;
        }

        public Integer getId() {
            return id;
        }

        public void setId(Integer id) {
            this.id = id;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getCategory() {
            return category;
        }

        public void setCategory(String category) {
            this.category = category;
        }

        public String getAuthor() {
            return author;
        }

        public void setAuthor(String author) {
            this.author = author;
        }

        public String getPublisher() {
            return publisher;
        }

        public void setPublisher(String publisher) {
            this.publisher = publisher;
        }

        public String getIsbn() {
            return isbn;
        }

        public void setIsbn(String isbn) {
            this.isbn = isbn;
        }

        public String getTags() {
            return tags;
        }

        public void setTags(String tags) {
            this.tags = tags;
        }
    }
}