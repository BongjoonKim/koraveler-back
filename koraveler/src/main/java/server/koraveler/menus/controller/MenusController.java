package server.koraveler.menus.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import server.koraveler.menus.dto.MenusDTO;
import server.koraveler.menus.service.MenusService;

import java.util.List;

@RestController
@RequestMapping("ps/menus")
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
            throw new ResponseStatusException(HttpStatus.valueOf("error"), e.getMessage());
        }
    }

    @GetMapping("")
    public ResponseEntity<MenusDTO> getMenu(
        @RequestParam("label") String label
    ) {
        try {
            return ResponseEntity.ok(menusService.getMenu(label));
        } catch (Exception e) {
            e.printStackTrace();
            throw new ResponseStatusException(HttpStatus.valueOf("error"), e.getMessage());
        }
    }

    @PostMapping("")
    public ResponseEntity<MenusDTO> createMenu(
            @RequestBody MenusDTO menusDTO
    ) {
        try {
            return ResponseEntity.ok(menusService.createMenu(menusDTO));
        } catch (Exception e) {
            e.printStackTrace();
            throw new ResponseStatusException(HttpStatus.valueOf("error"), e.getMessage());
        }
    }

    @PutMapping("")
    public ResponseEntity<MenusDTO> updateMenu(
            @RequestBody MenusDTO menusDTO
    ) {
        try {
            return ResponseEntity.ok(menusService.updateMenu(menusDTO));
        } catch (Exception e) {
            e.printStackTrace();
            throw new ResponseStatusException(HttpStatus.valueOf("error"), e.getMessage());
        }
    }

    @DeleteMapping("")
    public ResponseEntity<MenusDTO> deleteMenu(
            @RequestParam("label") String label
    ) {
        try {
            menusService.deleteMenu(label);
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            throw new ResponseStatusException(HttpStatus.valueOf("error"), e.getMessage());
        }
    }
}
