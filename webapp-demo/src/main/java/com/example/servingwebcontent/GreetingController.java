package com.example.servingwebcontent;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.rmi.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Vector;

import javax.annotation.Resource;

import com.example.servingwebcontent.beans.SessionToken;
import com.example.servingwebcontent.forms.User;  //dar import

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.util.HtmlUtils;


@Component
class rmiHandler{
    SearchModule_I h;
    Client c;
    
    public rmiHandler(){
        try {
            String ficheiro = "serverIp.txt";
            File myObj = new File(ficheiro);
            Scanner myReader = new Scanner(myObj);
            String data = myReader.nextLine();
            System.out.println(data);
            myReader.close();
            //String name = "rmi://" + "192.168.137.86" + ":1099/projeto";
            //Registry reg = LocateRegistry.getRegistry(data, 1099);
            Registry registry = LocateRegistry.getRegistry(data); // Replace SERVER_IP with the actual IP
            h = (SearchModule_I) registry.lookup("RemoteInterface");
            //h = (SearchModule_I) reg.lookup(name);
            //h = (SearchModule_I) Naming.lookup(name);
            c = new Client(); 
            h.subscribe("WebClient", c);
        } catch (/*MalformedURLException |*/ RemoteException | NotBoundException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
    }
}

@Controller
public class GreetingController {

    ArrayList<User> usersList = new ArrayList<User>();
    HashMap<String, Integer> searchedQuery = new HashMap<String, Integer>();

    @Autowired
    @Resource(name = "rmiHandler")
    private rmiHandler rmiHandler;

    @Autowired
    @Resource(name = "sessionScopedToken")
    private SessionToken token;

    @Bean
    @Scope(value = WebApplicationContext.SCOPE_SESSION, proxyMode = ScopedProxyMode.TARGET_CLASS)
    public SessionToken sessionScopedToken() {
        return new SessionToken();
    }

    @GetMapping("/")
    public String redirect(Model model) {
        //como manter o scope para a ligação rmi
        return "home";
    }

    @GetMapping("/statistics")
    public String showStatistics(Model model) {
        
        List<String> string = new ArrayList<String>();

        for (String key : searchedQuery.keySet()) {
            Integer value = searchedQuery.get(key);
            System.out.println("key " + key + " value " + value);
        }

        for (int i = 0; i < searchedQuery.size(); i++) {
            int max = 0;
            String tmp = "";
            for (String key : searchedQuery.keySet()) {
                Integer value = searchedQuery.get(key);
                if(!string.contains(key))
                    if(value>=max){
                        max = value;
                        tmp=key;
                    }
            }
            string.add(tmp);
        }
        
        for (String key : string) {
            System.out.println("aa key " + key);
        }

        model.addAttribute("orderedQueries", string);
        
        return "statistics";
    }

    @GetMapping("/searchTopStories")
	public String searchTopStories(Model model) {
		return "searchTopStories";
	}

    //query "with" encontra
    @PostMapping("/saveSearchTopStories")
	public String getTopStories(Model model, @RequestParam("queryStories")String queryStories) {
        List<String> querySplited = new ArrayList<String>(Arrays.asList(queryStories.split(" ")));
        String topStoriesEndpoint = "https://hacker-news.firebaseio.com/v0/topstories.json";
        List<HackerNewsItemRecord> list = hackerNewsTopStories(topStoriesEndpoint);
        List<String> urls = new ArrayList<String>();
        if(list==null){
            System.out.println("List topstories null");
            return "showTopStoryUrls";
        }
        for(HackerNewsItemRecord records : list){
            if(records.text()!=null && records.url()!=null){
                SearchModule_I h = rmiHandler.h;
                Client c = rmiHandler.c;
                List<String> textSplited = new ArrayList<String>(Arrays.asList(records.text().split(" ")));
                if(textSplited.containsAll(querySplited)){
                    try {
                        urls.add(records.url());
                        Client.acrescentar(h, c, records.url());
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        model.addAttribute("topStoryUrls", urls);
		return "showTopStoryUrls";
	}

    @GetMapping("/searchUserStories")
	public String searchUserStories(Model model) {
		return "searchUserStories";
	}
    //user gooseyard e 12345 para teste
    @PostMapping("/saveSearchUserStories")
	public String getUserStories(Model model, @RequestParam("queryUsers")String queryUsers) {
        model.addAttribute("queryUsers", queryUsers);
        String userStoriesEndpoint = "https://hacker-news.firebaseio.com/v0/user/" + queryUsers + ".json";
        List<HackerNewsItemRecord> list = hackerNewsUserStories(userStoriesEndpoint);
        List<String> urls = new ArrayList<String>();
        if(list == null || list.isEmpty()){
            System.out.println("List user null");
            return "showTopStoryUrls";
        }
        for(HackerNewsItemRecord records : list){
            if(records.url()!=null){
                SearchModule_I h = rmiHandler.h;
                Client c = rmiHandler.c;
                try {
                    urls.add(records.url());
                    Client.acrescentar(h, c, records.url());
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }
        model.addAttribute("userStoryUrls", urls);
		return "showUserStoryUrls";
	}

    private List<HackerNewsItemRecord> hackerNewsTopStories(String topStoriesEndPoint) {

        RestTemplate rest = new RestTemplate();
        List<Integer> hackerTopListId = (List<Integer>)(Object)rest.getForObject(topStoriesEndPoint, List.class);
        List<HackerNewsItemRecord> hackerTopList = new ArrayList<HackerNewsItemRecord>();
        if(hackerTopListId==null)
            return null;
        for (int i = 0; i < hackerTopListId.size(); i++) {
            String endPoint = "https://hacker-news.firebaseio.com/v0/item/" + hackerTopListId.get(i) + ".json";
            hackerTopList.add((HackerNewsItemRecord)rest.getForObject(endPoint, HackerNewsItemRecord.class));
        }
        
        return hackerTopList;
    }

    private List<HackerNewsItemRecord> hackerNewsUserStories(String userStoriesEndPoint) {

        RestTemplate rest = new RestTemplate();
        HackerNewsUsers hackerUserListId = (HackerNewsUsers)(Object)rest.getForObject(userStoriesEndPoint, HackerNewsUsers.class);
        List<HackerNewsItemRecord> hackerTopList = new ArrayList<HackerNewsItemRecord>();
        if(hackerUserListId==null)
            return null;
        for (int i = 0; i < hackerUserListId.submitted().size(); i++) {
            String endPoint = "https://hacker-news.firebaseio.com/v0/item/" + hackerUserListId.submitted().get(i) + ".json";
            hackerTopList.add((HackerNewsItemRecord)rest.getForObject(endPoint, HackerNewsItemRecord.class));
        }
        
        return hackerTopList;
    }

    private void printPretty(HackerNewsItemRecord tmp){
        System.out.println(
            "Id: " + tmp.id() + "\n"
            + "Deleted: " + tmp.deleted() + "\n"  
            + "Type: " + tmp.type() + "\n"
            + "Author: " + tmp.by() + "\n"
            + "Title: " + tmp.title() + "\n"
            + "Text:" + tmp.text()
            );
    }


    @GetMapping("/search")
	public String search(@RequestParam(name="token", required=true, defaultValue = "mario") String token, Model model) {
        if(!checkToken(token))
            return "goLogin";
		return "search";
	}

    @PostMapping("/saveSearch")
	public String sendSearch(Model model, @RequestParam("query")String query) {
		
        if(searchedQuery.get(query)==null)
            searchedQuery.put(query, 1);
        else
            searchedQuery.put(query, searchedQuery.get(query)+1);

        SearchModule_I searchModule = rmiHandler.h;
        Client client = rmiHandler.c;
        Vector<SaveInformationBarrel> searchResults;

        try {
            if(searchModule!=null && client!=null){
                searchResults = searchModule.search(query, "WebClient", client, true);
                model.addAttribute("searchResults", searchResults);
                if(searchResults!=null)
                    for (int i = 0; i < searchResults.size(); i++) {
                        System.out.println(searchResults.get(i).getUrl());
                    }
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }

		return "showSearch";
	}

    @GetMapping("/addUrl")
    public String addUrlForm(Model model){

        return "addUrl";
    }

    @PostMapping("/saveUrl")
    public String saveUrl(Model model, @RequestParam("url")String url){
        
        if(rmiHandler==null)
            System.out.println("SIUUU");
        SearchModule_I h = rmiHandler.h;
        Client c = rmiHandler.c;
        model.addAttribute("url", url);
        System.out.println("Url sent " + url);
        //String si = (String) model.getAttribute("url");
        try {
            Client.acrescentar(rmiHandler.h, c, url);
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        return "showUrl";
    }

    @GetMapping("/create-user")
    public String createUserForm(Model model) {
        
        model.addAttribute("user", new User());

        return "register";
    }

    @PostMapping("/save-user")
    public String showLoginPage(@ModelAttribute User user, Model model) {

        /*User tmp = (User)model.getAttribute("user");

        SecureRandom secureRandom = new SecureRandom(); //threadsafe
        Base64.Encoder base64Encoder = Base64.getUrlEncoder(); //threadsafe

        byte[] randomBytes = new byte[24];
        secureRandom.nextBytes(randomBytes);
        
        if(tmp!=null){
            tmp.setToken(base64Encoder.encodeToString(randomBytes));
            model.addAttribute("user", tmp);
        }*/
        User tmp = (User)model.getAttribute("user");
        if(checkUserExist(tmp)!=-1)
            return "usernameExists";

        usersList.add(tmp);
        System.out.println("User " + user.getUsername() + " added");

        return "registerSuccessful";
    }

    @GetMapping("/login")
    public String loginUserForm(@ModelAttribute User user, Model model) {
        
        return "login";
    }

    @PostMapping("/loginCheckExist")
    public String loginCheckExist(@ModelAttribute User user, Model model) {
        
        //User login = (User)model.getAttribute("user");
        if(user==null){
            System.out.println("User model null");
            return "redirect:/create-user";
        }
        int response = checkUserExist(user);
        if(response==-1)
            return "redirect:/create-user";
        else{
            if(!user.getPassword().equals(usersList.get(response).getPassword()))
                return "redirect:/create-user";
        }
        System.out.println(token.getValue());
        model.addAttribute("sessionToken", token.getValue());
        return "loginResults";
    }

	/*@GetMapping("/greeting")
	public String greeting(@RequestParam(name="name", required=false, defaultValue="World") String name,@RequestParam(name="disciplina", required=false, defaultValue="SD") String disciplina ,Model model) {
		model.addAttribute("name", name);
		model.addAttribute("othername", disciplina);
		return "greeting";
	}

    @GetMapping("/givemeatable")
	public String atable(Model model) {
        Employee [] theEmployees = { new Employee(1, "José", "9199999", 1890), new Employee(2, "Marisa", "9488444", 2120), new Employee(3, "Hélio", "93434444", 2500)};
        List<Employee> le = new ArrayList<>();
        Collections.addAll(le, theEmployees);
        model.addAttribute("emp", le);
		return "table";
	}

    // from https://attacomsian.com/blog/spring-boot-thymeleaf-form-handling and https://github.com/attacomsian/code-examples
	@GetMapping("/create-project")
    public String createProjectForm(Model model) {
        
        model.addAttribute("project", new Project());
        return "create-project";
    }

    @PostMapping("/save-project")
    public String saveProjectSubmission(@ModelAttribute Project project, Model model) {

        project = (Project) model.getAttribute("project");
        // TODO: save project in DB here

        return "result";
    }

    @GetMapping("/counters")
	public String counters(Model model) {
        ServletRequestAttributes attr = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        HttpSession session = attr.getRequest().getSession(true);
        Integer counter = (Integer) session.getAttribute("counter");
        int c;
        if (counter == null)
            c = 1;
        else
            c = counter + 1;
        session.setAttribute("counter", c);
		model.addAttribute("sessioncounter", c);
		model.addAttribute("requestcounter2", this.nRequest.next());
		model.addAttribute("sessioncounter2", this.nSession.next());
		model.addAttribute("applicationcounter2", this.nApplication.next());
		return "counter";
	}*/

    public int checkUserExist(User login){
        for (int i = 0; i < usersList.size(); i++) {
            if(login.getUsername().equals(usersList.get(i).getUsername()))
                return i;
        }
        return -1;
    }

    public boolean checkToken(String token){
        if(token.equals(this.token.getValue()))
            return true;
        else
            return false;
    }

}