import React, {useState, useEffect} from 'react';
import axios from 'axios'
import {makeStyles, withStyles} from '@material-ui/core/styles';
import List from '@material-ui/core/List';
import ListItem from '@material-ui/core/ListItem';
import ListItemText from '@material-ui/core/ListItemText';
import Button from '@material-ui/core/Button';
import Grid from '@material-ui/core/Grid';
import Fab from '@material-ui/core/Fab';
import AddIcon from '@material-ui/icons/Add';
import Box from '@material-ui/core/Box';
import CircularProgress from '@material-ui/core/CircularProgress';
import Breadcrumbs from '@material-ui/core/Breadcrumbs';
import Link from '@material-ui/core/Link';
import Modal from '@material-ui/core/Modal';

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
        height: '100%',
        backgroundColor: theme.palette.background.paper,
    },
    fabAddButton: {
        position: 'fixed',
        zIndex: 1,
        bottom: 30,
        right: 30,
        margin: '0 auto',
    },
    scroller: {
        display: 'flex',
        alignItems: 'center',
        justifyContent: "center",
    },
    confirm: {
        position: 'absolute',
        backgroundColor: theme.palette.background.paper,
        border: '2px solid #000',
        boxShadow: theme.shadows[5],
        padding: theme.spacing(2),
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

function ChatList() {
    // state
    const [chats, setChats] = useState([]);
    const [modalStyle] = useState(getModalStyle);
    const [openConfirmModal, setOpenConfirmModal] = useState(false);
    const [chatToDelete, setChatToDelete] = useState({});

    const fetchData = () => {
        axios
            .get(`/api/chat`)
            .then(message => {
                setChats(message.data);
            });
    };

    const openDeleteModal = (dto) => {
        setChatToDelete(dto);
        setOpenConfirmModal(true);
    };

    const handleCloseConfirmModal = () => {
        setOpenConfirmModal(false);
    };

    const onDelete = id => {
        console.log("Deleting", id);
        axios
            .delete(`/api/chat/${id}`)
            .then(() => {
                fetchData();
            });
    };

    const handleDelete = (id) => {
        onDelete(id);
        handleCloseConfirmModal();
    };

    useEffect(() => {
        fetchData();
    }, []);

    const classes = useStyles();

    let chatContent;
    if (Array.isArray(chats) && chats.length) {
        chatContent = (
            <List className="chat-list">
                {chats.map((value, index) => {
                    return (
                        <ListItem key={value.id} button>

                            <Grid container spacing={1} direction="row">
                                <Grid container item xs alignItems="center" spacing={1} className="downloadable-clickable">
                                    <ListItemText>
                                        <Box fontFamily="Monospace" className="list-element">
                                            {value.name}
                                        </Box>
                                    </ListItemText>
                                </Grid>

                                <Grid container item xs={2} direction="row"
                                      justify="flex-end"
                                      alignItems="center" spacing={1}>
                                    <Grid item>
                                        <Button variant="contained" color="primary">
                                            Share
                                        </Button>
                                    </Grid>
                                    <Grid item>
                                        <Button variant="contained" color="secondary" onClick={() => openDeleteModal(value)}>
                                            Delete
                                        </Button>
                                    </Grid>
                                </Grid>
                            </Grid>
                        </ListItem>
                    )
                })}
            </List>
        );
    } else {
        chatContent = (
        <div className={classes.scroller}>
            <CircularProgress size={72} thickness={8} variant={'indeterminate'} disableShrink={true}/>
        </div>
        );
    }

    return (
            <div className={classes.root}>
                <header className={classes.appHeader}>
                    <div className="header-text">Videochat</div>
                </header>
                <Breadcrumbs aria-label="breadcrumb">
                    <Link color="inherit" href="/">
                        Chats
                    </Link>
                    <Link color="inherit" href="/">
                        Current chat
                    </Link>
                </Breadcrumbs>

                <Fab color="primary" aria-label="add" className={classes.fabAddButton}>
                    <AddIcon className="fab-add"/>
                </Fab>

                {chatContent}


                <Modal
                    aria-labelledby="simple-modal-title"
                    aria-describedby="simple-modal-description"
                    open={openConfirmModal}
                    onClose={handleCloseConfirmModal}
                >
                    <div style={modalStyle} className={classes.confirm}>

                        <Grid container
                              direction="column"
                              justify="center"
                              alignItems="stretch"
                              spacing={2}>
                            <Grid item>
                                Confirm delete {chatToDelete.name}?
                            </Grid>

                            <Grid item container spacing={1}>
                                <Grid item>
                                    <Button variant="contained" color="primary"
                                            onClick={() => handleDelete(chatToDelete.id)}>
                                        Yes
                                    </Button>
                                </Grid>
                                <Grid item>

                                    <Button variant="contained" color="secondary" onClick={handleCloseConfirmModal}>
                                        Cancel
                                    </Button>
                                </Grid>
                            </Grid>
                        </Grid>
                    </div>
                </Modal>
            </div>

    );
}

export default (ChatList);