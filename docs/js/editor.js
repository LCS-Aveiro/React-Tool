const USER_MODELS_KEY = 'rta_user_custom_models';


function getSelectedUserModelName() {
    const selectedItem = document.querySelector('#project-tree .tree-item.selected');
    if (!selectedItem) return null;


    const isUserItem = selectedItem.innerText.includes('.rta'); 
    
    const rawName = selectedItem.innerText.replace('📄', '').replace('.rta', '').trim();
    
    let userModels = JSON.parse(localStorage.getItem(USER_MODELS_KEY) || '{}');
    return userModels[rawName] !== undefined ? rawName : null;
}

function saveUserModel() {
    const name = prompt(currentLang === 'pt' ? "Nome do modelo:" : "Model name:");
    if (!name) return;

    const cleanName = name.replace(".rta", "");
    let userModels = JSON.parse(localStorage.getItem(USER_MODELS_KEY) || '{}');
    userModels[cleanName] = editor.getValue();
    
    localStorage.setItem(USER_MODELS_KEY, JSON.stringify(userModels));
    
    updateProjectTree();
    alert(currentLang === 'pt' ? "Salvo com sucesso!" : "Saved successfully!");
}

function overwriteUserModel() {
    const name = getSelectedUserModelName();
    
    if (!name) {
        alert(currentLang === 'pt' ? 
            "Selecione um dos SEUS modelos (em 'My Models') para sobrescrever." : 
            "Select one of YOUR models (in 'My Models') to overwrite.");
        return;
    }

    if (confirm(currentLang === 'pt' ? `Deseja sobrescrever '${name}'?` : `Overwrite '${name}'?`)) {
        let userModels = JSON.parse(localStorage.getItem(USER_MODELS_KEY) || '{}');
        userModels[name] = editor.getValue();
        
        localStorage.setItem(USER_MODELS_KEY, JSON.stringify(userModels));
        alert(currentLang === 'pt' ? "Modelo atualizado!" : "Model updated!");
    }
}

function deleteUserModel() {
    const name = getSelectedUserModelName();
    
    if (!name) {
        alert(currentLang === 'pt' ? 
            "Você só pode excluir seus próprios modelos selecionados na lista." : 
            "You can only delete your own models selected in the list.");
        return;
    }

    if (confirm((currentLang === 'pt' ? "Excluir permanentemente " : "Permanently delete ") + name + "?")) {
        let userModels = JSON.parse(localStorage.getItem(USER_MODELS_KEY) || '{}');
        delete userModels[name];
        localStorage.setItem(USER_MODELS_KEY, JSON.stringify(userModels));
        
        editor.setValue("");
        document.getElementById('sb-model').textContent = "No model loaded";
        
        updateProjectTree();
    }
}

function createNewModel() {
    const name = prompt(currentLang === 'pt' ? "Nome do novo arquivo:" : "New file name:");
    if (!name) return;

    const fileName = name.replace(".rta", "");
    const template = `name ${fileName}\ninit s0\n\ns0 ---> s0: action`;

    let userModels = JSON.parse(localStorage.getItem(USER_MODELS_KEY) || '{}');
    userModels[fileName] = template;
    localStorage.setItem(USER_MODELS_KEY, JSON.stringify(userModels));

    updateProjectTree();
    
    loadModelFromTree(template, fileName);
    
    const ctxMenu = document.getElementById('project-context-menu');
    if (ctxMenu) ctxMenu.style.display = 'none';
}