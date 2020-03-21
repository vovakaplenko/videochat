import React, {useState, useEffect} from 'react';
import axios from 'axios'
import Modal from '@material-ui/core/Modal';
import {makeStyles, withStyles} from '@material-ui/core/styles';
import List from '@material-ui/core/List';
import ListItem from '@material-ui/core/ListItem';
import ListItemText from '@material-ui/core/ListItemText';
import Button from '@material-ui/core/Button';
import Grid from '@material-ui/core/Grid';
import TextField from '@material-ui/core/TextField';
import Fab from '@material-ui/core/Fab';
import AddIcon from '@material-ui/icons/Add';
import Fade from '@material-ui/core/Fade';
import Box from '@material-ui/core/Box';
import { green, common } from '@material-ui/core/colors';
import Popover from '@material-ui/core/Popover';
import Typography from '@material-ui/core/Typography';
import CircularProgress from '@material-ui/core/CircularProgress';
import BackupIcon from '@material-ui/icons/Backup';
import Breadcrumbs from '@material-ui/core/Breadcrumbs';
import Link from '@material-ui/core/Link';
import Keycloak from 'keycloak-js';

const circleCheckRadius = 34;
const useStyles = makeStyles(theme => ({
    appHeader: {
        'background-color': '#282c34',
        display: 'flex',
        'flex-direction': 'column',
        'align-items': 'center',
        'justify-content': 'center',
        'font-size': 'calc(10px + 2vmin)',
        color: 'white',
        'word-wrap': 'break-word',
        'font-family': 'monospace',
    },
    root: {
        width: '100%',
        backgroundColor: theme.palette.background.paper,
    },
    fabAddButton: {
        position: 'absolute',
        zIndex: 1,
        bottom: 30,
        right: 30,
        margin: '0 auto',
    },
    fabRestoreButton: {
        position: 'absolute',
        zIndex: 1,
        bottom: 30,
        left: 30,
        margin: '0 auto',
    },
    paper: {
        position: 'absolute',
        width: 400,
        backgroundColor: theme.palette.background.paper,
        border: '2px solid #000',
        boxShadow: theme.shadows[5],
        padding: theme.spacing(2),
    },
    confirm: {
        position: 'absolute',
        backgroundColor: theme.palette.background.paper,
        border: '2px solid #000',
        boxShadow: theme.shadows[5],
        padding: theme.spacing(2),
    },
    typography: {
        padding: theme.spacing(2),
    },
    buttonProgress: {
        color: green[500],
        position: 'absolute',
        top: '50%',
        left: '50%',
        marginTop: -circleCheckRadius,
        marginLeft: -circleCheckRadius,
    },
}));

function getModalStyle() {
    const top = 50;
    const left = 50;

    return {
        top: `${top}%`,
        left: `${left}%`,
        transform: `translate(-${top}%, -${left}%)`,
    };
}

const GreenButton = withStyles(theme => ({
    root: {
        color: common[500],
        backgroundColor: green[500],
        '&:hover': {
            backgroundColor: green[700],
        },
    },
}))(Button);

function Chat() {
    // state
    const [connections, setConnections] = useState([]);
    const [modalStyle] = React.useState(getModalStyle);
    const [openEditModal, setOpenEditModal] = React.useState(false);
    const [editDto, setEditDto] = React.useState({});
    const [valid, setValid] = React.useState(true);
    const [openConfirmModal, setOpenConfirmModal] = React.useState(false);
    const [dbToDelete, setDbToDelete] = React.useState({});
    const [checkPopoverAnchorEl, checkPopoverSetAnchorEl] = React.useState(null);
    const [checkMessage, setCheckMessage] = React.useState("");
    const [disableWhileChecking, setDisableWhileChecking] = React.useState(false);
    const [openUploadModal, setOpenUploadModal] = React.useState(false);
    const [uploadEnabled, setUploadEnabled] = React.useState(false);

    const [keycloak, setKeycloak] = useState(null);
    const [authenticated, setAuthenticated] = useState(null);

    const fetchData = () => {
        console.log("before get");
        axios.get(`/chat`)
            .then(message => {
                const m = message.data;
                setConnections(m);
            });
    };

    useEffect(() => {
        const keycloak = Keycloak({
            realm: "secured-backend-realm",
            "auth-server-url": "http://auth.site.local:8080/auth",
            "ssl-required": "external",
            clientId: "backend-app",
            "public-client": true,
            "confidential-port": 0
        });
        keycloak.init({onLoad: 'login-required'}).then(authenticated => {
            setKeycloak(keycloak);
            setAuthenticated(authenticated);
        });
        console.log("Keycloak initialized");

        fetchData();
    }, []);

    const classes = useStyles();

    const onDelete = id => {
        console.log("Deleting", id);
        axios.delete(`/chat/${id}`)
            .then(() => {
                fetchData();
            });
    };

    const onSave = (c, event) => {
        checkPopoverSetAnchorEl(event.currentTarget);
        setCheckMessage("Checking...");

        (c.id ? axios.put(`/chat`, c) : axios.post(`/chat`, c))
            .then(() => {
                fetchData();
                handleCloseEditModal();
                handleCheckPopoverClose();
            })
            .catch((error) => {
                // handle error
                console.log("Handling error on save", error.response);
                setCheckMessage(error.response.data.message);
            });
    };


    const handleCheck = (dto, event) => {
        checkPopoverSetAnchorEl(event.currentTarget);
        setCheckMessage("Checking...");
        setDisableWhileChecking(true);

        axios.post(`check`, {"connectionUrl": dto.connectionUrl})
            .then((resp) => {
                setDisableWhileChecking(false);
                setCheckMessage(resp.data.message);
            });
    };

    const handleCheckPopoverClose = () => {
        if (disableWhileChecking) {
            console.log("You cannot close popover during checking");
            return
        }
        checkPopoverSetAnchorEl(null);
        setCheckMessage("");
    };

    const handleEditModalOpen = (c) => {
        console.log("Editing modal", c.id);
        setEditDto(c);
        validate(c);
        setOpenEditModal(true);
    };

    const handleCloseEditModal = () => {
        if (disableWhileChecking) {
            console.log("You cannot close modal during checking");
            return
        }
        setOpenEditModal(false);
    };

    const handleCloseConfirmModal = () => {
        setOpenConfirmModal(false);
    };

    const handleUploadModalOpen = () => {
        setUploadEnabled(false);
        setOpenUploadModal(true);
    };
    
    const handleCloseUploadModal = () => {
        setOpenUploadModal(false);
    };

    const formOnChange = (e) => {
        console.log("Form changed", e.target.value);
        if (e.target.value) {
            setUploadEnabled(true);
        }
    };
    
    const validString = s => {
        if (s) {
            return true
        } else {
            return false
        }
    };

    const validate = (dto) => {
        let v = validString(dto.name) && validString(dto.connectionUrl);
        console.log("Valid? " + JSON.stringify(dto) + " : " + v);
        setValid(v)
    };

    const handleChangeName = event => {
        const dto = {...editDto, name: event.target.value};
        setEditDto(dto);
        validate(dto);
    };

    const handleChangeConnectionUrl = event => {
        const dto = {...editDto, connectionUrl: event.target.value};
        setEditDto(dto);
        validate(dto);
    };

    const handleDump = id => {
        const d = "dump/" + id;
        console.log(`Will open ${window.location.href + d} for download gzipped file`);
        window.open(d, '_blank');
    };

    const openDeleteModal = (dto) => {
        setDbToDelete(dto);
        setOpenConfirmModal(true);
    };

    const handleDelete = (id) => {
        onDelete(id);
        handleCloseConfirmModal();
    };

    const id = open ? 'simple-popover' : undefined;
    const open = Boolean(checkPopoverAnchorEl);

    if (keycloak) {
        if (authenticated) return (
            <div>
                <p>This is a Keycloak-secured component of your application. You shouldn't be able
                    to see this unless you've authenticated with Keycloak.</p>
            </div>
        ); else return (<div>Unable to authenticate!</div>)
    }
    return (
        <div>Initializing Keycloak...</div>
    );
}

export default (Chat);
