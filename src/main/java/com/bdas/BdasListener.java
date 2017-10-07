package com.bdas;

import com.atlassian.event.api.EventListener;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.event.issue.IssueEvent;
import com.atlassian.jira.event.type.EventType;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.mail.Email;
import com.atlassian.mail.MailException;
import com.atlassian.mail.server.SMTPMailServer;
import com.atlassian.plugin.spring.scanner.annotation.export.ExportAsService;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import javax.inject.Inject;
import javax.inject.Named;

@ExportAsService({BdasListener.class})
@Named("bdaslistener")
public class BdasListener implements InitializingBean, DisposableBean {
    private final EventPublisher eventPublisher;

    private static final Logger log = LoggerFactory.getLogger(BdasListener.class);

    @Inject
    public BdasListener(@ComponentImport EventPublisher eventPublisher) {
        this.eventPublisher= eventPublisher;
    }

    public void destroy() throws Exception {
        eventPublisher.unregister(this);
    }

    public void afterPropertiesSet() throws Exception {
        eventPublisher.register(this);
    }

    @EventListener
    public void onIssueEvent(IssueEvent issueEvent) {
        Long eventTypeId = issueEvent.getEventTypeId();
        Issue issue = issueEvent.getIssue();

        //send email if created issue belongs to TEST project and has the highest priority 
        if (eventTypeId.equals(EventType.ISSUE_CREATED_ID) && issue.getKey().contains("TEST") && issue.getPriority().getId().equals("1"))
        {
            sendEmail("abstractelement@mail.ru","Blocker Priority " + issue.getKey(), issue.getSummary());
            log.info("BDAS Listener: Mail from listener has been sent - " + issue.getKey());
        }
    }

    public void sendEmail(String emailAdr, String subject, String body) {
        SMTPMailServer smtpMailServer = ComponentAccessor.getMailServerManager().getDefaultSMTPMailServer();
        if (smtpMailServer != null) {
            Email email = new Email(emailAdr);
            email.setSubject(subject);
            email.setBody(body);
            try {
                smtpMailServer.send(email);
                log.debug("BDAS Listener: Mail sent");
            } catch (MailException e) {
                log.error(e.getMessage());
            }
        }
        else {
            log.warn("BDAS Listener: Check SMTPMailServer configuration");
        }
    }
}
