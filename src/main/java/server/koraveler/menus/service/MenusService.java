package server.koraveler.menus.service;

import server.koraveler.menus.dto.MenusDTO;

import java.util.List;

public interface MenusService {
    List<MenusDTO> getAllMenus() throws Exception;
    MenusDTO getMenu(String label) throws Exception;
    MenusDTO createMenu(MenusDTO menusDTO) throws Exception;
    MenusDTO updateMenu(MenusDTO menusDTO) throws Exception;
    void deleteMenu(String label) throws Exception;

}
