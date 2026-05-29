// Owner: HeenuReet | Feedback | JpaRepository for feedback retrieval by user, product, or date
package com.shopping.system.repository;

import com.shopping.system.entity.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FeedbackRepository extends JpaRepository<Feedback, Long> {

    List<Feedback> findByUserId(Long userId);

    List<Feedback> findByProductId(Long productId);

    List<Feedback> findAllByOrderByFeedbackDateDesc();
}
