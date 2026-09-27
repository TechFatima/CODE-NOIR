package com.codenoir.demo.controller;

import com.codenoir.demo.model.OnboardingReport;
import com.codenoir.demo.service.AnalysisService;
import com.codenoir.demo.service.AnalysisService.ExtractionResult;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

@Controller
public class UploadController {

	private final AnalysisService analysisService;

	public UploadController(AnalysisService analysisService) {
		this.analysisService = analysisService;
	}

	@GetMapping("/")
	public String index() {
		return "index";
	}

	@PostMapping("/analyze")
	public String analyze(@RequestParam("file") MultipartFile file, Model model) {
		model.addAttribute("filename", file.getOriginalFilename());
		try {
			ExtractionResult extraction = analysisService.extract(file);
			model.addAttribute("fileCount", extraction.fileCount());
			model.addAttribute("projectName", extraction.projectName());
			model.addAttribute("extractedPath", extraction.directory().toString());

			analysisService.analyze(extraction.directory());
			OnboardingReport report = analysisService.readReport(extraction.directory());
			model.addAttribute("report", report);
		} catch (Exception e) {
			model.addAttribute("error", e.getMessage());
		}
		return "index";
	}
}
