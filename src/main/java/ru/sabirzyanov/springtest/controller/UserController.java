package ru.sabirzyanov.springtest.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.propertyeditors.CustomDateEditor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import ru.sabirzyanov.springtest.domain.History;
import ru.sabirzyanov.springtest.domain.Role;
import ru.sabirzyanov.springtest.domain.User;
import ru.sabirzyanov.springtest.service.HistoryService;
import ru.sabirzyanov.springtest.service.UserService;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;


@Controller
@RequestMapping("/user")
public class UserController {
    @Autowired
    private UserService userService;

    @Autowired
    private HistoryService historyService;

    @PreAuthorize("hasAuthority('ADMIN')")
    @GetMapping
    public String userList(@RequestParam(required = false, defaultValue = "") String username,
                                                                              Model model,
                           @PageableDefault(sort = { "id" }, direction = Sort.Direction.DESC) Pageable pageable
    ) {

        userService.userListCreator(model, pageable, username);

        return "user";
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @GetMapping("{user}")
    public String userEditForm(@PathVariable User user, Model model) {
        model.addAttribute("user", user);
        model.addAttribute("roles", Role.values());
        model.addAttribute("oldUsername", user.getUsername());
        model.addAttribute("oldEmail", user.getEmail());
        model.addAttribute("oldActivatedPoints", 0);

        return "userEdit";
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @PostMapping
    public String addPoints(@AuthenticationPrincipal User admin,
                            Model model,
                            @RequestParam String usernamePost,
                            @RequestParam Long discount,
                            @RequestParam Long points,
                            @PageableDefault(sort = { "id" }, direction = Sort.Direction.DESC) Pageable pageable
                            ) {

        userService.userListCreator(model, pageable, usernamePost);
        model.addAttribute("pointErrorUsername", usernamePost);

        if (points < 0 && (discount < 0 || discount > 100)) {
            model.addAttribute("pointsError", "Point can't be negative!");
            model.addAttribute("discountError", "From 0 to 100");
        } else if (points < 0){
            model.addAttribute("pointsError", "Point can't be negative!");
        } else if (discount < 0 || discount > 100) {
            model.addAttribute("discountError", "From 0 to 100");
        } else {
            userService.addPoints(usernamePost, discount, points, admin);
            return "redirect:user";
        }

        /*List<User> userList = new ArrayList<>();
        userList.add(userService.findUser(usernamePost));
        model.addAttribute("usersList", userList);*/

        return "user";
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @PostMapping(value = "{user}", params = "save")
    public String userSave(
            @AuthenticationPrincipal User admin,
            Model model,
            @RequestParam String username,
            @RequestParam String email,
            @RequestParam Map<String, String> form,
            @RequestParam Long activatedPoints,
            @PathVariable @RequestParam("userId") User user
    ) {
        model.addAttribute("user", user);
        model.addAttribute("roles", Role.values());
        model.addAttribute("oldUsername", username);
        model.addAttribute("oldEmail", email);
        model.addAttribute("oldActivatedPoints", activatedPoints);

        if (userService.findUser(username) == null || user.getUsername().equals(username)) {
            if (userService.findUserByEmail(email) == null || user.getEmail().equals(email)) {
                userService.saveUser(user, username, email, admin, form, model);
            }
            else {
                model.addAttribute("emailError", "A user with same email already exist");
                return "userEdit";
            }
        }
        else {
            model.addAttribute("usernameError", "A user with same username already exist");
            return "userEdit";
        }

        return "userEdit";
    }


    /*
    *  500 баллов = 1 услуга
    * */
    @PreAuthorize("hasAuthority('ADMIN')")
    @PostMapping(value = "{user}", params = "activatePoints")
    public String activatePoints(
            @AuthenticationPrincipal User admin,
            @PathVariable @RequestParam("userId") User user,
            @RequestParam Long activatedPoints,
            Model model
    ) {
        model.addAttribute("user", user);
        model.addAttribute("roles", Role.values());
        model.addAttribute("oldUsername", user.getUsername());
        model.addAttribute("oldEmail", user.getEmail());
        model.addAttribute("oldActivatedPoints", activatedPoints);

        if (activatedPoints == 0) {
            return "redirect:/user/" + user.getId();
        }
        userService.activatePoints(user, admin, activatedPoints, model);
        return "userEdit";
        //return "redirect:/user/" + user.getId();
    }

    @InitBinder
    public void initBinder(WebDataBinder binder) throws Exception {
        final DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        final CustomDateEditor dateEditor = new CustomDateEditor(df, true) {
            @Override
            public void setAsText(String text) throws IllegalArgumentException {
                if ("today".equals(text)) {
                    setValue(new Date());
                } else {
                    super.setAsText(text);
                }
            }
        };
        binder.registerCustomEditor(Date.class, dateEditor);
    }

    @GetMapping("profile")
    public String getProfile(
            Model model,
            @AuthenticationPrincipal User user,
            @RequestParam(required = false, defaultValue = "2000-01-01") @DateTimeFormat(pattern="yyyy-MM-dd") Date dateFrom,
            @RequestParam(required = false, defaultValue = "today") @DateTimeFormat(pattern="yyyy-MM-dd") Date dateTo,
            Pageable pageable
    ) {
        Page<History> page;
        page = historyService.findUserDate(user.getId(), dateFrom, dateTo, pageable);

        model.addAttribute("username", user.getUsername());
        model.addAttribute("email", user.getEmail());
        model.addAttribute("score", user.getScore());
        model.addAttribute("page", page);

        return "profile";
    }

    @PostMapping("profile")
    public String updateProfile(
            @AuthenticationPrincipal User user,
            Model model,
            @RequestParam("password2") String passwordConfirmation,
            @RequestParam String password,
            @RequestParam String email,
            @RequestParam(required = false, defaultValue = "2000-01-01") @DateTimeFormat(pattern="yyyy-MM-dd") Date dateFrom,
            @RequestParam(required = false, defaultValue = "today") @DateTimeFormat(pattern="yyyy-MM-dd") Date dateTo,
            Pageable pageable
    ) {
        Page<History> page;
        page = historyService.findUserDate(user.getId(), dateFrom, dateTo, pageable);

        model.addAttribute("username", user.getUsername());
        model.addAttribute("email", user.getEmail());
        model.addAttribute("score", user.getScore());
        model.addAttribute("page", page);

        if (userService.findUserByEmail(email) != null && !email.equals(user.getEmail())) {
            model.addAttribute("email", email);
            model.addAttribute("emailError", "Email exist");
            return "profile";
        }
        if (StringUtils.isEmpty(password) && StringUtils.isEmpty(passwordConfirmation))
            userService.updateProfile(user, "", email, model);
        else if (!StringUtils.isEmpty(password)) {
            boolean isConfirmEmpty = StringUtils.isEmpty(passwordConfirmation);
            if (isConfirmEmpty) {
                model.addAttribute("password2Error", "Password confirmation can not be empty");
                return "profile";
            }

            if (!password.equals(passwordConfirmation)) {
                model.addAttribute("passwordError", "Passwords are different");
                return "profile";
            }
            userService.updateProfile(user, password, email, model);
        }

        return "profile";
    }
}
