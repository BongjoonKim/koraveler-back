package server.koraveler.menus.service.serviceImpl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import server.koraveler.menus.dto.MenusDTO;
import server.koraveler.menus.model.Menus;
import server.koraveler.menus.repo.MenusRepo;
import server.koraveler.menus.service.MenusService;
import server.koraveler.users.repo.UsersRepo;
import server.koraveler.users.service.UsersService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Slf4j
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

    @Override
    public MenusDTO getMenu(String label) throws Exception {
        try {
            Menus menu = menusRepo.findByLabel(label);
            MenusDTO menusDTO = new MenusDTO();
            BeanUtils.copyProperties(menu, menusDTO);
            return menusDTO;
        } catch (Exception e) {
            throw e;
        }
    }

    @Override
    public MenusDTO createMenu(MenusDTO menusDTO) throws Exception {
        try {
            Menus menus = new Menus();
            BeanUtils.copyProperties(menusDTO, menus);

            // 라벨 명이 있는지
            if (menusDTO.getLabel() == null) {
                throw new ResponseStatusException(HttpStatus.valueOf("none"), "Please we need label name");
            }
            // 이미 존재하는 라벨이 있는지 확인
            if (menusRepo.findByLabel(menusDTO.getLabel()) != null) {
                throw new ResponseStatusException(HttpStatus.valueOf("duplicated"), "Duplicated label exists");
            }

            LocalDateTime now = LocalDateTime.now();
            menus.setCreated(now);
            menus.setUpdated(now);
            Menus newMenus = menusRepo.insert(menus);
            MenusDTO newMenusDTO = new MenusDTO();

            BeanUtils.copyProperties(newMenus, newMenusDTO);
            return newMenusDTO;
        } catch (Exception e) {
            throw e;
        }
    }

    @Override
    public MenusDTO updateMenu(MenusDTO menusDTO) throws Exception {
        try {
            Menus menus = new Menus();
            BeanUtils.copyProperties(menusDTO, menus);

            // 라벨 명이 있는지
            if (menusDTO.getLabel() == null) {
                throw new ResponseStatusException(HttpStatus.valueOf("none"), "Please we need label name");
            }
            // 이미 존재하는 라벨이 있는지 확인
            if (menusRepo.findByLabel(menusDTO.getLabel()) != null) {
                throw new ResponseStatusException(HttpStatus.valueOf("duplicated"), "Duplicated label exists");
            }

            LocalDateTime now = LocalDateTime.now();
            menus.setUpdated(now);
            Menus newMenus = menusRepo.save(menus);
            MenusDTO newMenusDTO = new MenusDTO();

            BeanUtils.copyProperties(newMenus, newMenusDTO);
            return newMenusDTO;
        } catch (Exception e) {
            throw e;
        }
    }

    @Override
    public void deleteMenu(String label) throws Exception {
        try {
            Menus menu = menusRepo.findByLabel(label);
            if (menu == null) {
                throw new ResponseStatusException(HttpStatus.valueOf("none"), "there is no label name");
            }
            menusRepo.delete(menu);
        } catch (Exception e) {
            throw e;
        }
    }
}
