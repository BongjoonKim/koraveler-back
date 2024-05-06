package server.koraveler.menus.service;

import server.koraveler.menus.dto.MenusDTO;

import java.util.List;

public interface MenusService {
    List<MenusDTO> getAllMenus() throws Exception;
}
