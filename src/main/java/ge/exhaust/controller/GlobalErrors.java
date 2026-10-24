package ge.exhaust.controller;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@ControllerAdvice
public class GlobalErrors {
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String tooLarge(RedirectAttributes flash) {
        flash.addFlashAttribute("error","The upload was too large. Please use a JPG or PNG photo smaller than 5 MB.");
        return "redirect:/admin";
    }
}
