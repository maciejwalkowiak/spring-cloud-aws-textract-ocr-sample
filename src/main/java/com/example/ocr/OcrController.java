package com.example.ocr;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.stream.Collectors;

import io.awspring.cloud.s3.ObjectMetadata;
import io.awspring.cloud.s3.S3Template;
import software.amazon.awssdk.services.textract.TextractClient;
import software.amazon.awssdk.services.textract.model.Block;
import software.amazon.awssdk.services.textract.model.BlockType;
import software.amazon.awssdk.services.textract.model.Document;

import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

@Controller
@RequestMapping
class OcrController {
    private final S3Template s3Template;
    private final AppProperties appProperties;
    private final TextractClient textractClient;

    OcrController(S3Template s3Template, AppProperties appProperties, TextractClient textractClient) {
        this.s3Template = s3Template;
        this.appProperties = appProperties;
        this.textractClient = textractClient;
    }

    @GetMapping("upload")
    String index() {
        return "index";
    }

    @PostMapping("upload")
    String upload(@RequestParam("file") MultipartFile multipartFile, Model model) throws IOException {
        if (StringUtils.hasLength(multipartFile.getOriginalFilename())) {
            var id = UUID.randomUUID().toString();
            s3Template.upload(appProperties.bucket(), multipartFile.getOriginalFilename(),
                    multipartFile.getInputStream(),
                    ObjectMetadata.builder().contentType(multipartFile.getContentType()).build());

            var ocr = ocr(multipartFile.getOriginalFilename());

            s3Template.upload(appProperties.bucket(), id,
                    new ByteArrayInputStream(ocr.getBytes(StandardCharsets.UTF_8)),
                    ObjectMetadata.builder().contentType("text/plain").build());

            model.addAttribute("ocr", ocr);
            model.addAttribute("downloadLink",
                    UriComponentsBuilder.fromHttpUrl(appProperties.baseUrl()).pathSegment("download").pathSegment(id)
                            .build());
        }
        return "index";
    }

    @GetMapping("download/{id}")
    @ResponseBody
    ResponseEntity<Resource> translationDocument(@PathVariable("id") String id) {
        var resource = s3Template.download(appProperties.bucket(), id);
        return ResponseEntity.ok().contentType(MediaType.valueOf(resource.contentType())).body(resource);
    }

    private String ocr(String fileName) {
        return textractClient.detectDocumentText(r -> r.document(
                        Document.builder().s3Object(s3 -> s3.bucket(appProperties.bucket()).name(fileName))
                                .build()))
                .blocks()
                .stream().filter(block -> block.blockType() == BlockType.LINE)
                .map(Block::text)
                .collect(Collectors.joining("\n"));
    }

}
