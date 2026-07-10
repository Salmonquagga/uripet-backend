package com.dbp.uripet.events;

import com.dbp.uripet.events.email.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.thymeleaf.context.Context;

@Component("customEventListener")
@RequiredArgsConstructor
public class UripetEventListener {
    private final EmailService emailService;

    @EventListener
    public void handleUserRegistered(UserRegisteredEvent event) {
        Context context = new Context();
        context.setVariable("code", event.getVerificationCode());
        emailService.sendHtmlEmail(event.getUserEmail(), "Verifica tu cuenta de UriPet", "VerificationCode", context);
    }

    @EventListener
    public void handleUserVerified(UserVerifiedEvent event) {
        Context context = new Context();
        context.setVariable("userName", event.getUserName());
        emailService.sendHtmlEmail(event.getUserEmail(), "Te damos la bienvenida a UriPet", "WelcomeMessage", context);
    }

    @EventListener
    public void handleUserAddedToPet(UserAddedToPetEvent event) {
        Context context = new Context();
        context.setVariable("petName", event.getPetName());
        emailService.sendHtmlEmail(event.getUserEmail(), "Asignacion de Cuidador - UriPet", "AddResponsible", context);
    }

    @EventListener
    public void handleBusinessVerificationRequested(BusinessVerificationRequestedEvent event) {
        Context context = new Context();
        context.setVariable("requestUid", event.getRequestUid());
        context.setVariable("requesterName", event.getRequesterName());
        context.setVariable("requesterEmail", event.getRequesterEmail());
        context.setVariable("businessName", event.getBusinessName());
        context.setVariable("businessType", event.getBusinessType());

        emailService.sendHtmlEmail(
                event.getRequesterEmail(),
                "Solicitud de verificacion recibida - UriPet",
                "BusinessVerificationRequested",
                context
        );
    }

    @EventListener
    public void handleBusinessVerificationApproved(BusinessVerificationApprovedEvent event) {
        Context context = new Context();
        context.setVariable("userName", event.getUserName());
        context.setVariable("businessName", event.getBusinessName());
        context.setVariable("businessType", event.getBusinessType());
        context.setVariable("workspaceUid", event.getWorkspaceUid());

        emailService.sendHtmlEmail(
                event.getUserEmail(),
                "Solicitud aprobada - UriPet",
                "BusinessVerificationApproved",
                context
        );
    }

    @EventListener
    public void handleBusinessVerificationRejected(BusinessVerificationRejectedEvent event) {
        Context context = new Context();
        context.setVariable("userName", event.getUserName());
        context.setVariable("businessName", event.getBusinessName());
        context.setVariable("businessType", event.getBusinessType());
        context.setVariable("reviewComment", event.getReviewComment());

        emailService.sendHtmlEmail(
                event.getUserEmail(),
                "Solicitud rechazada - UriPet",
                "BusinessVerificationRejected",
                context
        );
    }
}
