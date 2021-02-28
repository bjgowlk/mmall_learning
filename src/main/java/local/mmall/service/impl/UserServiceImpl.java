package local.mmall.service.impl;

import local.mmall.common.Const;
import local.mmall.common.ServerResponse;
import local.mmall.common.TokenCache;
import local.mmall.dao.UserMapper;
import local.mmall.pojo.User;
import local.mmall.service.IUserService;
import local.mmall.util.MD5Util;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Created by cat on 2/24/21.
 */
@Service("iUserService")
public class UserServiceImpl implements IUserService{

    @Autowired
    private UserMapper userMapper;

    @Override
    public ServerResponse<User> login(String username, String password) {
        int resultCount = userMapper.checkUsername(username);
        if(resultCount == 0){
            return ServerResponse.createByErrorMessage("User not exist.");
        }

        String md5Password = MD5Util.MD5EncodeUtf8(password);
        User user = userMapper.selectLogin(username, md5Password);
        if(user == null){
            return ServerResponse.createByErrorMessage("Password not match.");
        }

        user.setPassword(StringUtils.EMPTY);
        return ServerResponse.createBySuccess("Login success.", user);

    }

    @Override
    public ServerResponse<String> register(User user){

        ServerResponse validResponse = this.checkValid(user.getUsername(),Const.USERNAME);
        if(!validResponse.isSuccess()){
            return validResponse;
        }

        validResponse = this.checkValid(user.getEmail(),Const.EMAIL);
        if(!validResponse.isSuccess()){
            return validResponse;
        }

        user.setRole(Const.Role.ROLE_CUSTOMER);

        //password md5 hash
        user.setPassword(MD5Util.MD5EncodeUtf8(user.getPassword()));
        int resultCount = userMapper.insert(user);
        if(resultCount == 0){
            return ServerResponse.createByErrorMessage("Register failed.");
        }
        return ServerResponse.createBySuccessMessage("Register success.");
    }

    @Override
    public ServerResponse<String> checkValid(String str, String type){
        if(StringUtils.isNoneBlank()){
            // start check
            if(Const.USERNAME.equals(type)){
                int resultCount = userMapper.checkUsername(str);
                if(resultCount > 0){
                    return ServerResponse.createByErrorMessage("User has existed.");
                }
            }
            if(Const.EMAIL.equals(type)){
                int resultCount = userMapper.checkEmail(str);
                if(resultCount > 0){
                    return ServerResponse.createByErrorMessage("Email has existed.");
                }

            }
        }
        else{
            return ServerResponse.createByErrorMessage("Parameters error.");
        }
        return ServerResponse.createBySuccessMessage("Valid.");
    }

    @Override
    public ServerResponse<String> selectQuestion(String username) {
        ServerResponse validResponse = this.checkValid(username, Const.USERNAME);
        if (!validResponse.isSuccess()) {
            return ServerResponse.createByErrorMessage("User not found.");
        }
        String question = userMapper.selectQuestionByUsername(username);
        if (!StringUtils.isNoneBlank(question)) {
            return ServerResponse.createBySuccess(question);
        }
        return ServerResponse.createByErrorMessage("Have no password recovery question.");
    }

    @Override
    public ServerResponse<String> checkAnswer(String username, String question, String answer){
        int resultCount = userMapper.checkAnswer(username, question, answer);
        if(resultCount>0){
            String forgetToken = UUID.randomUUID().toString();
            TokenCache.setKey(TokenCache.TOKEN_PREFIX + username, forgetToken);
            return ServerResponse.createBySuccess(forgetToken);
        }
        return null;
    }

    @Override
    public ServerResponse<String> forgetResetPassword(String username, String passwordNew, String forgetToken){
        if(StringUtils.isBlank(forgetToken)){
            return ServerResponse.createByErrorMessage("Token is null.");
        }

        ServerResponse validResponse = this.checkValid(username, Const.USERNAME);
        if (!validResponse.isSuccess()) {
            return ServerResponse.createByErrorMessage("User not found.");
        }

        String token = TokenCache.getkey(TokenCache.TOKEN_PREFIX + username);
        if(StringUtils.isBlank(token)){
            return ServerResponse.createByErrorMessage("Token not valid.");
        }

        if(StringUtils.equals(forgetToken, token)){
            String md5Password = MD5Util.MD5EncodeUtf8(passwordNew);
            int rowCount = userMapper.updatePasswordByUsername(username, md5Password);
            if(rowCount > 0){
                return ServerResponse.createBySuccessMessage("Password reset successfully.");
            }
        }
        else{
            return ServerResponse.createByErrorMessage("Token not valid.");
        }
        return ServerResponse.createByErrorMessage("Password not changed.");
    }

    public ServerResponse<String> resetPassword(String passwordOld, String passwordNew, User user){
        int resultCount = userMapper.checkPassword(passwordOld,user.getId());
        if(resultCount == 0){
            return ServerResponse.createByErrorMessage("Old password not correct.");
        }
        user.setPassword(MD5Util.MD5EncodeUtf8(passwordNew));
        int updateCount = userMapper.updateByPrimaryKeySelective(user);
        if(updateCount>0){
            return ServerResponse.createByErrorMessage("Password updated!");
        }
        return ServerResponse.createByErrorMessage("Password not changed.");
    }

    public ServerResponse<User> updateInformation(User user){
        //username cannot change.
        //email check.
        int resultCount = userMapper.checkEmailByUserId(user.getEmail(), user.getId());
        if(resultCount > 0){
            ServerResponse.createByErrorMessage("Email has been used by another user.");
        }
        User updateUser = new User();
        updateUser.setId(user.getId());
        updateUser.setEmail(user.getEmail());
        updateUser.setPhone(user.getPhone());
        updateUser.setQuestion(user.getQuestion());
        updateUser.setAnswer(user.getAnswer());

        int updateCount = userMapper.updateByPrimaryKeySelective(updateUser);
        if(updateCount > 0){
            return ServerResponse.createBySuccess("Update user info successfully.", updateUser);
        }
        return ServerResponse.createByErrorMessage("Update user info failed.");
    }

    public ServerResponse<User> getInformation(Integer userId){
        User user = userMapper.selectByPrimaryKey(userId);
        {
            if(user == null){
                return ServerResponse.createByErrorMessage("User not found.");
            }
            user.setPassword(StringUtils.EMPTY);
            return ServerResponse.createBySuccess(user);
        }
    }
    public static void main(String[] args){
        System.out.println(UUID.randomUUID().toString());
    }
}
