// Owner: HeenuReet | Feedback | Service for submitting and retrieving user product feedback
package com.shopping.system.service;

import com.shopping.system.entity.Feedback;
import com.shopping.system.entity.Product;
import com.shopping.system.entity.User;
import com.shopping.system.repository.FeedbackRepository;
import com.shopping.system.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class FeedbackService {

    @Autowired
    private FeedbackRepository feedbackRepository;

    @Autowired
    private ProductRepository productRepository;

    public Feedback submitFeedback(User user, Long productId, int rating, String comment) {
        Feedback feedback = new Feedback();
        feedback.setUser(user);
        feedback.setRating(rating);
        feedback.setComment(comment);

        if (productId != null) {
            Optional<Product> product = productRepository.findById(productId);
            product.ifPresent(feedback::setProduct);
        }

        return feedbackRepository.save(feedback);
    }

    public List<Feedback> getFeedbackByUser(Long userId) {
        return feedbackRepository.findByUserId(userId);
    }

    public List<Feedback> getAllFeedback() {
        return feedbackRepository.findAllByOrderByFeedbackDateDesc();
    }

    public List<Feedback> getFeedbackByProduct(Long productId) {
        return feedbackRepository.findByProductId(productId);
    }
}
