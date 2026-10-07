package com.nqt.football_management.specification;

import com.nqt.football_management.entity.User;
import com.nqt.football_management.model.request.AdminUserPageRequest;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.Locale;

public final class UserSpecification {
    private UserSpecification() { }

    public static Specification<User> filter(AdminUserPageRequest request) {
        return (root, query, cb) -> {
            var predicates = new ArrayList<Predicate>();
            if (request.getStatus() != null) predicates.add(cb.equal(root.get("status"), request.getStatus()));
            if (request.getRole() != null) predicates.add(cb.equal(root.get("role"), request.getRole()));
            if (request.getSearch() != null && !request.getSearch().isBlank()) {
                String search = request.getSearch().trim().toLowerCase(Locale.ROOT)
                        .replace("!", "!!").replace("%", "!%").replace("_", "!_");
                String pattern = "%" + search + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("email")), pattern, '!'),
                        cb.like(cb.lower(root.get("fullName")), pattern, '!'),
                        cb.like(cb.lower(root.get("code")), pattern, '!'),
                        cb.like(cb.lower(root.get("phone")), pattern, '!')));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
