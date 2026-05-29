package com.feedback.service.report;

import com.feedback.service.model.FeedbackItem;
import com.feedback.service.repository.FeedbackRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class WeeklyReportService {

    private static final Logger log = LoggerFactory.getLogger(WeeklyReportService.class);

    private final FeedbackRepository feedbackRepository;

    public WeeklyReportService(FeedbackRepository feedbackRepository) {
        this.feedbackRepository = feedbackRepository;
    }

    @Scheduled(cron = "${report.cron:0 0 8 * * MON}")
    public void generateWeeklyReport() {
        LocalDate endDate = LocalDate.now(ZoneOffset.UTC);
        LocalDate startDate = endDate.minusDays(7);

        String start = startDate.atStartOfDay(ZoneOffset.UTC).toInstant().toString();
        String end = endDate.atStartOfDay(ZoneOffset.UTC).toInstant().toString();

        List<FeedbackItem> feedbacks = feedbackRepository.findByDateRange(start, end);

        if (feedbacks.isEmpty()) {
            log.info("Weekly report: no feedbacks in period {} to {}", startDate, endDate);
            return;
        }

        double mediaNotas = feedbacks.stream()
                .mapToInt(FeedbackItem::getNota)
                .average()
                .orElse(0.0);

        Map<String, Long> porDia = feedbacks.stream()
                .collect(Collectors.groupingBy(
                        f -> f.getDataEnvio().substring(0, 10),
                        Collectors.counting()
                ));

        Map<String, Long> porUrgencia = feedbacks.stream()
                .collect(Collectors.groupingBy(
                        FeedbackItem::getUrgencia,
                        Collectors.counting()
                ));

        StringBuilder report = new StringBuilder();
        report.append("=== RELATÓRIO SEMANAL DE FEEDBACKS ===\n");
        report.append(String.format("Período: %s a %s\n", startDate, endDate));
        report.append(String.format("Total de avaliações: %d\n", feedbacks.size()));
        report.append(String.format("Média das notas: %.2f\n\n", mediaNotas));

        report.append("Avaliações por dia:\n");
        porDia.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(e -> report.append(String.format("  %s: %d\n", e.getKey(), e.getValue())));

        report.append("\nAvaliações por urgência:\n");
        porUrgencia.forEach((k, v) -> report.append(String.format("  %s: %d\n", k, v)));

        log.info("\n{}", report);
    }
}
