package com.prestobr.financeiro.config;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice // intercepra exceções em controllers e retorna JSON ao invés de HTML
public class GlobalExceptionHandler {

    // Erros de validação (@Valid, @NotBlank, @Size, etc.)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new HashMap<>();

        ex.getBindingResult().getFieldErrors().forEach(error ->
                fieldErrors.put(error.getField(), error.getDefaultMessage())
        );

        Map<String, Object> response = new HashMap<>();
        response.put("status", HttpStatus.BAD_REQUEST.value());
        response.put("error", "Validation failed");
        response.put("fields", fieldErrors);

        return ResponseEntity.badRequest().body(response);
    }

    // ResponseStatusException (exceções lançadas manualmente)
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatus(ResponseStatusException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", ex.getStatusCode().value());
        response.put("error", ex.getReason());

        return ResponseEntity.status(ex.getStatusCode()).body(response);
    }

    // Corpo da requisição ausente ou com JSON malformado.
    // A mensagem da exceção carrega detalhe interno do parser, então não é
    // repassada ao cliente.
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleUnreadableBody(HttpMessageNotReadableException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", HttpStatus.BAD_REQUEST.value());
        response.put("error", "Corpo da requisição ausente ou com JSON inválido");

        return ResponseEntity.badRequest().body(response);
    }

    // Qualquer outra exceção não tratada
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        // As exceções do próprio Spring MVC (rota inexistente, método não
        // suportado, parâmetro obrigatório ausente) implementam ErrorResponse e
        // já trazem o status correto. Sem este desvio, o catch-all abaixo as
        // transformava em 500, e o cliente não conseguia distinguir um erro na
        // requisição dele de uma falha do servidor.
        if (ex instanceof ErrorResponse errorResponse) {
            HttpStatusCode status = errorResponse.getStatusCode();

            Map<String, Object> response = new HashMap<>();
            response.put("status", status.value());
            response.put("error", ex.getMessage());

            return ResponseEntity.status(status).body(response);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        response.put("error", "Internal server error");
        response.put("message", ex.getMessage());

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
