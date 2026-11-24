package com.example.albaondo_public;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.example.albaondo_public.global.error.ApiExceptionHandler;
import com.example.albaondo_public.global.security.AuthenticatedUser;
import com.example.albaondo_public.review.controller.ReviewController;
import com.example.albaondo_public.review.domain.ReviewStatus;
import com.example.albaondo_public.review.repository.ReviewRepository;
import com.example.albaondo_public.review.service.ReviewService;
import com.example.albaondo_public.user.domain.User;
import com.example.albaondo_public.user.domain.UserRole;
import com.example.albaondo_public.user.repository.UserRepository;
import com.example.albaondo_public.user.service.ActiveUserProvider;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
    properties = "spring.datasource.url=jdbc:h2:mem:review-crud-check;MODE=MySQL")
class ReviewApiTest {
    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import({ReviewController.class, ReviewService.class, ActiveUserProvider.class})
    static class Application {}

    @Autowired ReviewController controller;
    @Autowired UserRepository users;
    @Autowired ReviewRepository reviews;
    MockMvc mvc;

    @BeforeEach
    void setup() {
        reviews.deleteAll();
        mvc = MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new ApiExceptionHandler())
            .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
            .build();
    }

    @AfterEach
    void clearPrincipal() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void listContainsFullContentAndMineIsScoped() throws Exception {
        UUID first = newUser();
        UUID second = newUser();
        long firstId = create(first, "first");
        long secondId = create(second, "second");
        authenticate(first);

        mvc.perform(get("/api/reviews").param("size", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items[0].reviewId").value(secondId))
            .andExpect(jsonPath("$.items[0].mine").value(false))
            .andExpect(jsonPath("$.items[0].author").doesNotExist())
            .andExpect(jsonPath("$.items[0].authorId").doesNotExist())
            .andExpect(jsonPath("$.hasNext").value(true));
        mvc.perform(get("/api/reviews").param("page", "1").param("size", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items[0].reviewId").value(firstId))
            .andExpect(jsonPath("$.hasNext").value(false));
        mvc.perform(get("/api/reviews/mine"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items.length()").value(1))
            .andExpect(jsonPath("$.items[0].title").value("first"))
            .andExpect(jsonPath("$.items[0].content").value("full review content"))
            .andExpect(jsonPath("$.items[0].workYear").value(2025))
            .andExpect(jsonPath("$.items[0].mine").value(true));
    }

    @Test
    void onlyOwnerCanModifyAndDeleteIsIdempotent() throws Exception {
        UUID owner = newUser();
        long id = create(owner, "original");
        authenticate(newUser());
        mvc.perform(put("/api/reviews/{id}", id).contentType(MediaType.APPLICATION_JSON)
            .content(body("forbidden")))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("REVIEW_NOT_OWNED"));
        mvc.perform(delete("/api/reviews/{id}", id)).andExpect(status().isForbidden());
        authenticate(owner);
        mvc.perform(put("/api/reviews/{id}", id).contentType(MediaType.APPLICATION_JSON)
            .content("{\"title\":\"changed\",\"content\":\"changed content\",\"workYear\":2026}"))
            .andExpect(status().isNoContent());
        mvc.perform(get("/api/reviews/mine"))
            .andExpect(jsonPath("$.items[0].title").value("changed"))
            .andExpect(jsonPath("$.items[0].content").value("changed content"))
            .andExpect(jsonPath("$.items[0].workYear").value(2026));
        mvc.perform(delete("/api/reviews/{id}", id)).andExpect(status().isNoContent());
        mvc.perform(delete("/api/reviews/{id}", id)).andExpect(status().isNoContent());
        assertEquals(ReviewStatus.HIDDEN, reviews.findById(id).orElseThrow().getStatus());
        mvc.perform(get("/api/reviews")).andExpect(jsonPath("$.items.length()").value(0));
        mvc.perform(get("/api/reviews/mine")).andExpect(jsonPath("$.items.length()").value(0));
        mvc.perform(put("/api/reviews/{id}", id).contentType(MediaType.APPLICATION_JSON)
            .content(body("cannot restore")))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("REVIEW_NOT_EDITABLE"));
    }

    @Test
    void rejectsInvalidInputAndDetailHasNoGetMapping() throws Exception {
        UUID owner = newUser();
        long id = create(owner, "original");
        mvc.perform(put("/api/reviews/{id}", id).contentType(MediaType.APPLICATION_JSON)
            .content("{\"title\":\" \",\"content\":\"changed\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.validation.title").exists())
            .andExpect(jsonPath("$.validation.workYear").exists());
        assertEquals("original", reviews.findById(id).orElseThrow().getTitle());
        mvc.perform(get("/api/reviews").param("size", "51"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_REVIEW"))
            .andExpect(jsonPath("$.validation.size").exists());
        mvc.perform(get("/api/reviews/{id}", id)).andExpect(status().isMethodNotAllowed());
    }

    private UUID newUser() {
        return users.save(BeanUtils.instantiateClass(User.class)).getId();
    }

    private void authenticate(UUID id) {
        var principal = new AuthenticatedUser(id, id.toString(), UserRole.USER);
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(principal, null, List.of())
        );
    }

    private long create(UUID id, String title) throws Exception {
        authenticate(id);
        mvc.perform(post("/api/reviews").contentType(MediaType.APPLICATION_JSON).content(body(title)))
            .andExpect(status().isCreated())
            .andExpect(content().string(""));
        return reviews.findByAuthor_IdAndStatus(id, ReviewStatus.PUBLISHED,
            org.springframework.data.domain.Pageable.unpaged()).getContent().getFirst().getReviewId();
    }

    private String body(String title) {
        return "{\"title\":\"" + title + "\",\"content\":\"full review content\",\"workYear\":2025}";
    }
}
