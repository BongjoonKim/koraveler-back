package server.koraveler.menus.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import server.koraveler.menus.dto.MenusDTO;
import server.koraveler.menus.service.MenusService;

import java.util.List;

@RestController
@RequestMapping("/menus")
@Slf4j
public class MenusController {
    @Autowired
    private MenusService menusService;

    @GetMapping("/all")
    public ResponseEntity<List<MenusDTO>> getAllMenus() {
        try {
            return ResponseEntity.ok(menusService.getAllMenus());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.ofNullable(null);
        }
    }
}
