package server.koraveler.menus.service.serviceImpl;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import server.koraveler.menus.dto.MenusDTO;
import server.koraveler.menus.model.Menus;
import server.koraveler.menus.repo.MenusRepo;
import server.koraveler.menus.service.MenusService;
import server.koraveler.users.repo.UsersRepo;
import server.koraveler.users.service.UsersService;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class MenusServiceImpl implements MenusService {
    @Autowired
    private MenusRepo menusRepo;

    @Override
    public List<MenusDTO> getAllMenus() throws Exception {
        try {
            List<Menus> menus = menusRepo.findAll();
            List<MenusDTO> menusDTOList = new ArrayList<>();

            menus.stream().sorted(
                    Comparator.comparing(Menus::getSequence)
            );

            menus.stream().forEach(menu -> {
                MenusDTO menusDTO = new MenusDTO();
                BeanUtils.copyProperties(menu, menusDTO);
                menusDTOList.add(menusDTO);
            });
            return menusDTOList;
        } catch (Exception e) {
            throw e;
        }
    }
}
