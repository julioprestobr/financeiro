package com.prestobr.financeiro.infra;

import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.util.ArrayList;
import java.util.List;

// Classe utilitária com métodos auxiliares para operações no AWS S3
public class S3Utils {

    public static List<String> listKeys(
            S3Client s3Client,
            String bucket,
            String prefix
    ) {

        List<String> keys = new ArrayList<>();

        // Monta a requisição de listagem com o bucket e o prefixo desejados
        ListObjectsV2Request request = ListObjectsV2Request.builder()
                .bucket(bucket)
                .prefix(prefix)
                .build();

        // listObjectsV2Paginator retorna um iterável de páginas.
        // Para cada página, itera sobre os objetos e adiciona a chave na lista.
        for (ListObjectsV2Response page : s3Client.listObjectsV2Paginator(request)) { // S3 limita a 1000 objetos por página
            for (S3Object obj : page.contents()) {
                keys.add(obj.key()); // obj.key() retorna o caminho completo do arquivo no S3
            }
        }

        return keys;
    }
}
