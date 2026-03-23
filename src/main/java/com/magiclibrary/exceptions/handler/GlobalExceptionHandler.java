package com.magiclibrary.exceptions.handler;

// -----------------------------------------------------------------------------
// IMPORTS STANDARD JAVA
// -----------------------------------------------------------------------------
import java.time.LocalDateTime;

// -----------------------------------------------------------------------------
// IMPORTS SERVLET
// -----------------------------------------------------------------------------
import jakarta.servlet.http.HttpServletRequest;

// -----------------------------------------------------------------------------
// IMPORTS SPRING WEB
// -----------------------------------------------------------------------------
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

// -----------------------------------------------------------------------------
// IMPORTS VALIDATION
// -----------------------------------------------------------------------------
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;

// -----------------------------------------------------------------------------
// IMPORTS SPRING RESOURCE
// -----------------------------------------------------------------------------
import org.springframework.web.servlet.resource.NoResourceFoundException;

// -----------------------------------------------------------------------------
// IMPORTS SWAGGER / OPENAPI
// -----------------------------------------------------------------------------
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

// -----------------------------------------------------------------------------
// IMPORTS INTERNES MAGICLIBRARY
// -----------------------------------------------------------------------------
import com.magiclibrary.exceptions.custom.ContactAlreadyAnsweredException;
import com.magiclibrary.exceptions.custom.EmailAlreadyExistsException;
import com.magiclibrary.exceptions.custom.ForbiddenException;
import com.magiclibrary.exceptions.custom.InvalidCredentialsException;
import com.magiclibrary.exceptions.custom.InvalidJwtException;
import com.magiclibrary.exceptions.custom.InvalidLoanLineStatusException;
import com.magiclibrary.exceptions.custom.ItemNotFoundException;
import com.magiclibrary.exceptions.custom.ItemUnavailableException;
import com.magiclibrary.exceptions.custom.LoanAlreadyReturnedException;
import com.magiclibrary.exceptions.custom.LoanLineNotFoundException;
import com.magiclibrary.exceptions.custom.LoanLineValidationException;
import com.magiclibrary.exceptions.custom.LoanNotFoundException;
import com.magiclibrary.exceptions.custom.NotificationNotFoundException;
import com.magiclibrary.exceptions.custom.RoleNotFoundException;
import com.magiclibrary.exceptions.custom.UnauthorizedException;
import com.magiclibrary.exceptions.custom.UserNotFoundException;
import com.magiclibrary.exceptions.model.ApiErrorResponse;

/**
 * =============================================================================
 *  GLOBAL EXCEPTION HANDLER
 * =============================================================================
 *  Classe centrale pour intercepter et formater toutes les exceptions lancées
 *  dans l’application MagicLibrary.
 *
 *  Objectifs :
 *      - fournir un format homogène JSON pour toutes les erreurs REST
 *      - renvoyer un code HTTP et un message clair
 *      - éviter la fuite de stack trace ou d’informations sensibles
 *      - centraliser la gestion des erreurs liées à :
 *          • routes et ressources
 *          • validation Bean / paramètres
 *          • authentification / autorisation
 *          • règles métier et conflits
 *          • erreurs inattendues (500)
 *
 *  Responsabilité :
 *      - uniquement formatage et mapping exception → ApiErrorResponse
 *      - aucune logique métier
 * =============================================================================
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    // =========================================================================
    // MÉTHODES UTILITAIRES
    // =========================================================================
    /**
     * Crée un ApiErrorResponse standardisé pour renvoyer au client REST.
     *
     * @param status  code HTTP
     * @param message message lisible
     * @param request requête HTTP originale
     * @return ApiErrorResponse complet
     */
    private ApiErrorResponse buildErrorResponse(
            HttpStatus status,
            String message,
            HttpServletRequest request
    ) {
        return new ApiErrorResponse(
                LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                request.getRequestURI()
        );
    }

    /**
     * Détermine si la requête attend prioritairement une réponse HTML.
     *
     * @param request requête HTTP originale
     * @return true si la requête semble provenir d’un navigateur
     */
    private boolean expectsHtml(HttpServletRequest request) {
        String acceptHeader = request.getHeader("Accept");

        if (acceptHeader == null || acceptHeader.isBlank()) {
            return false;
        }

        return acceptHeader.contains("text/html");
    }

    /**
     * Construit une vue HTML d’erreur SSR.
     *
     * @param viewName nom de la vue Thymeleaf
     * @param status   code HTTP
     * @return ModelAndView configuré
     */
    private ModelAndView buildHtmlErrorView(String viewName, HttpStatus status) {
        ModelAndView modelAndView = new ModelAndView(viewName);
        modelAndView.setStatus(status);
        return modelAndView;
    }

    // =========================================================================
    // 0) ERREURS ROUTES / MÉTHODES
    // =========================================================================

    /** Route inconnue (404 Not Found) */
    @ApiResponses({
            @ApiResponse(
                    responseCode = "404",
                    description = "Ressource introuvable",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            )
    })
    @ExceptionHandler(NoResourceFoundException.class)
    public Object handleNoResourceFound(
            NoResourceFoundException ex,
            HttpServletRequest request
    ) {
        if (expectsHtml(request)) {
            return buildHtmlErrorView("error/404", HttpStatus.NOT_FOUND);
        }

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(buildErrorResponse(
                        HttpStatus.NOT_FOUND,
                        "Ressource introuvable.",
                        request
                ));
    }

    /** Méthode HTTP non autorisée (405 Method Not Allowed) */
    @ApiResponses({
            @ApiResponse(
                    responseCode = "405",
                    description = "Méthode HTTP non autorisée",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            )
    })
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public Object handleMethodNotSupported(
            HttpRequestMethodNotSupportedException ex,
            HttpServletRequest request
    ) {
        if (expectsHtml(request)) {
            return buildHtmlErrorView("error/405", HttpStatus.METHOD_NOT_ALLOWED);
        }

        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(buildErrorResponse(
                        HttpStatus.METHOD_NOT_ALLOWED,
                        "Méthode HTTP non autorisée pour cette ressource.",
                        request
                ));
    }

    /** JSON invalide (400 Bad Request) */
    @ApiResponses({
            @ApiResponse(
                    responseCode = "400",
                    description = "Corps de requête invalide",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            )
    })
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Object handleMessageNotReadable(
            HttpMessageNotReadableException ex,
            HttpServletRequest request
    ) {
        if (expectsHtml(request)) {
            return buildHtmlErrorView("error/400", HttpStatus.BAD_REQUEST);
        }

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(buildErrorResponse(
                        HttpStatus.BAD_REQUEST,
                        "Corps de requête invalide ou JSON mal formé.",
                        request
                ));
    }

    /** Paramètre ou valeur invalide (400 Bad Request) */
    @ApiResponses({
            @ApiResponse(
                    responseCode = "400",
                    description = "Paramètre ou donnée invalide",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            )
    })
    @ExceptionHandler(IllegalArgumentException.class)
    public Object handleIllegalArgument(
            IllegalArgumentException ex,
            HttpServletRequest request
    ) {
        String message = (ex.getMessage() != null && !ex.getMessage().isBlank())
                ? ex.getMessage()
                : "Requête invalide.";

        if (expectsHtml(request)) {
            return buildHtmlErrorView("error/400", HttpStatus.BAD_REQUEST);
        }

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(buildErrorResponse(HttpStatus.BAD_REQUEST, message, request));
    }

    /** Validation Bean / DTO (400 Bad Request) */
    @ApiResponses({
            @ApiResponse(
                    responseCode = "400",
                    description = "Erreur de validation des données",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            )
    })
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Object handleValidationException(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        String message = "Requête invalide.";

        if (!ex.getBindingResult().getAllErrors().isEmpty()) {
            FieldError fieldError = (FieldError) ex.getBindingResult()
                    .getAllErrors().get(0);
            message = fieldError.getDefaultMessage();
        }

        if (expectsHtml(request)) {
            return buildHtmlErrorView("error/400", HttpStatus.BAD_REQUEST);
        }

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(buildErrorResponse(HttpStatus.BAD_REQUEST, message, request));
    }

    /** Validation des paramètres (400 Bad Request) */
    @ApiResponses({
            @ApiResponse(
                    responseCode = "400",
                    description = "Erreur de validation des paramètres",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            )
    })
    @ExceptionHandler(ConstraintViolationException.class)
    public Object handleConstraintViolation(
            ConstraintViolationException ex,
            HttpServletRequest request
    ) {
        String message = "Paramètres invalides.";

        if (ex.getConstraintViolations() != null && !ex.getConstraintViolations().isEmpty()) {
            ConstraintViolation<?> violation = ex.getConstraintViolations().iterator().next();
            if (violation != null && violation.getMessage() != null) {
                message = violation.getMessage();
            }
        }

        if (expectsHtml(request)) {
            return buildHtmlErrorView("error/400", HttpStatus.BAD_REQUEST);
        }

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(buildErrorResponse(HttpStatus.BAD_REQUEST, message, request));
    }

    // =========================================================================
    // 1) AUTHENTIFICATION / JWT (401 Unauthorized)
    // =========================================================================

    @ExceptionHandler({
            InvalidJwtException.class,
            InvalidCredentialsException.class,
            UnauthorizedException.class,
            BadCredentialsException.class
    })
    public ResponseEntity<ApiErrorResponse> handleAuthExceptions(
            Exception ex,
            HttpServletRequest request
    ) {
        String message = ex instanceof BadCredentialsException
                ? "Identifiants invalides"
                : ex.getMessage();

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(buildErrorResponse(HttpStatus.UNAUTHORIZED, message, request));
    }

    // =========================================================================
    // 2) AUTORISATION (403 Forbidden)
    // =========================================================================

    @ExceptionHandler({ForbiddenException.class, AccessDeniedException.class})
    public Object handleForbiddenExceptions(
            Exception ex,
            HttpServletRequest request
    ) {
        String message = ex instanceof AccessDeniedException
                ? "Accès interdit."
                : ex.getMessage();

        if (expectsHtml(request)) {
            return buildHtmlErrorView("error/403", HttpStatus.FORBIDDEN);
        }

        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(buildErrorResponse(HttpStatus.FORBIDDEN, message, request));
    }

    // =========================================================================
    // 3) MÉTIER / CONFLITS / VALIDATION FONCTIONNELLE (404, 409, 422)
    // =========================================================================

    @ExceptionHandler({
            ContactAlreadyAnsweredException.class,
            EmailAlreadyExistsException.class,
            UserNotFoundException.class,
            RoleNotFoundException.class,
            NotificationNotFoundException.class,
            ItemNotFoundException.class,
            LoanNotFoundException.class,
            LoanAlreadyReturnedException.class,
            LoanLineNotFoundException.class,
            ItemUnavailableException.class,
            LoanLineValidationException.class,
            InvalidLoanLineStatusException.class
    })
    public Object handleBusinessExceptions(
            Exception ex,
            HttpServletRequest request
    ) {
        HttpStatus status;

        if (ex instanceof ContactAlreadyAnsweredException
                || ex instanceof EmailAlreadyExistsException
                || ex instanceof LoanAlreadyReturnedException
                || ex instanceof ItemUnavailableException) {
            status = HttpStatus.CONFLICT;
        } else if (ex instanceof UserNotFoundException
                || ex instanceof RoleNotFoundException
                || ex instanceof NotificationNotFoundException
                || ex instanceof ItemNotFoundException
                || ex instanceof LoanNotFoundException
                || ex instanceof LoanLineNotFoundException) {
            status = HttpStatus.NOT_FOUND;
        } else if (ex instanceof LoanLineValidationException
                || ex instanceof InvalidLoanLineStatusException) {
            status = HttpStatus.UNPROCESSABLE_ENTITY;
        } else {
            status = HttpStatus.BAD_REQUEST;
        }

        if (expectsHtml(request)) {
            if (status == HttpStatus.CONFLICT) {
                return buildHtmlErrorView("error/409", HttpStatus.CONFLICT);
            }

            if (status == HttpStatus.NOT_FOUND) {
                return buildHtmlErrorView("error/404", HttpStatus.NOT_FOUND);
            }

            if (status == HttpStatus.UNPROCESSABLE_ENTITY) {
                return buildHtmlErrorView("error/422", HttpStatus.UNPROCESSABLE_ENTITY);
            }

            return buildHtmlErrorView("error/400", HttpStatus.BAD_REQUEST);
        }

        return ResponseEntity.status(status)
                .body(buildErrorResponse(status, ex.getMessage(), request));
    }

    // =========================================================================
    // 4) ERREURS D’INFRA / DISPONIBILITÉ (503, 504, 502)
    // =========================================================================

    @ExceptionHandler(java.util.concurrent.TimeoutException.class)
    public Object handleTimeoutException(
            java.util.concurrent.TimeoutException ex,
            HttpServletRequest request
    ) {
        if (expectsHtml(request)) {
            return buildHtmlErrorView("error/504", HttpStatus.GATEWAY_TIMEOUT);
        }

        return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT)
                .body(buildErrorResponse(
                        HttpStatus.GATEWAY_TIMEOUT,
                        "Le service a mis trop de temps à répondre.",
                        request
                ));
    }

    @ExceptionHandler(IllegalStateException.class)
    public Object handleIllegalStateException(
            IllegalStateException ex,
            HttpServletRequest request
    ) {
        String message = (ex.getMessage() != null && !ex.getMessage().isBlank())
                ? ex.getMessage()
                : "Le service est temporairement indisponible.";

        if (expectsHtml(request)) {
            return buildHtmlErrorView("error/503", HttpStatus.SERVICE_UNAVAILABLE);
        }

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(buildErrorResponse(
                        HttpStatus.SERVICE_UNAVAILABLE,
                        message,
                        request
                ));
    }

    @ExceptionHandler(UnsupportedOperationException.class)
    public Object handleUnsupportedOperationException(
            UnsupportedOperationException ex,
            HttpServletRequest request
    ) {
        String message = (ex.getMessage() != null && !ex.getMessage().isBlank())
                ? ex.getMessage()
                : "Le service amont a renvoyé une réponse invalide.";

        if (expectsHtml(request)) {
            return buildHtmlErrorView("error/502", HttpStatus.BAD_GATEWAY);
        }

        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(buildErrorResponse(
                        HttpStatus.BAD_GATEWAY,
                        message,
                        request
                ));
    }

    // =========================================================================
    // 5) ERREUR INATTENDUE (500 Internal Server Error)
    // =========================================================================

    @ExceptionHandler(Exception.class)
    public Object handleGenericException(
            Exception ex,
            HttpServletRequest request
    ) {
        if (expectsHtml(request)) {
            return buildHtmlErrorView("error/500", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(buildErrorResponse(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "Une erreur interne est survenue. Merci de réessayer plus tard.",
                        request
                ));
    }
}