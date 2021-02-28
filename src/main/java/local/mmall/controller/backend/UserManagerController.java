package local.mmall.controller.backend;

import local.mmall.common.Const;
import local.mmall.common.ServerResponse;
import local.mmall.pojo.User;
import local.mmall.service.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpSession;

/**
 * Created by cat on 2/28/21.
 */

@Controller
@RequestMapping("/manager/user")
public class UserManagerController {
    @Autowired
    private IUserService iUserService;

    @RequestMapping(value="login.do",method= RequestMethod.POST)
    public ServerResponse<User> login(String username, String password, HttpSession session){
        ServerResponse<User> response = iUserService.login(username, password);
        if(response.isSuccess()){
            User user = response.getData();
            if(user.getRole() == Const.Role.ROLE_ADMIN){
                //is Admin
                session.setAttribute(Const.CURRENT_USER, user);
                return response;
            }
            return ServerResponse.createByErrorMessage("Not admin, cannot login.");
        }
        return response;
    }


}
