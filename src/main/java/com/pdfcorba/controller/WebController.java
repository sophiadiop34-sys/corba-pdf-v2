package com.pdfcorba.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class WebController {

    @GetMapping(value = {"", "/"})
    public String home(Model model) {
        model.addAttribute("pageTitle", "CORBA PDF Service");
        return "index";
    }

    @GetMapping("/manage")
    public String manage(Model model) {
        model.addAttribute("pageTitle", "Gestion PDF");
        return "manage";
    }

    @GetMapping("/analyze")
    public String analyze(Model model) {
        model.addAttribute("pageTitle", "Analyse PDF");
        return "analyze";
    }

    @GetMapping("/transform")
    public String transform(Model model) {
        model.addAttribute("pageTitle", "Transformation & Sécurité");
        return "transform";
    }

    @GetMapping("/create")
    public String create(Model model) {
        model.addAttribute("pageTitle", "Créer un PDF");
        return "create";
    }
}